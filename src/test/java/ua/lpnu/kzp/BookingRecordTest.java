package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingRecordTest {
    private static final LocalDate IN = LocalDate.of(2026, 1, 1);
    private static final LocalDate OUT = LocalDate.of(2026, 1, 4);

    private static BookingRecord sample() {
        return new BookingRecord(101, "John Doe", IN, OUT, 150.0);
    }

    // ---------- Модель ----------

    @Test
    void gettersReturnConstructorValues() {
        BookingRecord record = sample();
        assertEquals(101, record.getRoom());
        assertEquals("John Doe", record.getGuest());
        assertEquals(IN, record.getCheckIn());
        assertEquals(OUT, record.getCheckOut());
        assertEquals(150.0, record.getRate(), 0.0001);
        assertTrue(record.toString().contains("John Doe"));
    }

    @Test
    void constructorRejectsInvalidState() {
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(0, "A", IN, OUT, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, " ", IN, OUT, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, null, IN, OUT, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, "A", null, OUT, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, "A", IN, null, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, "A", IN, OUT, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, "A", IN, OUT, Double.NaN));
    }

    @Test
    void checkOutMustBeAfterCheckIn() {
        assertThrows(IllegalArgumentException.class, () -> new BookingRecord(1, "A", IN, IN, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new BookingRecord(1, "A", IN, IN.minusDays(1), 1.0));
        assertEquals(IN.plusDays(1), new BookingRecord(1, "A", IN, IN.plusDays(1), 1.0).getCheckOut());
    }

    @Test
    void fromBookingUsesNightsForCheckOutForBothSubtypes() {
        BookingRecord standard = BookingRecord.fromBooking(
                Booking.fromCsv("101;John Doe;3;150.00;Standard"), IN);
        BookingRecord premium = BookingRecord.fromBooking(
                Booking.fromCsv("301;Dexter Morgan;7;1000.00;Suite"), IN);
        assertEquals(sample(), standard);
        assertEquals(new BookingRecord(301, "Dexter Morgan", IN, IN.plusDays(7), 1000.0), premium);
    }

    @Test
    void equalsAndHashCodeUseAllFields() {
        BookingRecord record = sample();
        assertEquals(record, sample());
        assertEquals(record.hashCode(), sample().hashCode());
        assertEquals(record, record);
        assertNotEquals(null, record);
        assertNotEquals("text", record);
        assertNotEquals(record, new BookingRecord(102, "John Doe", IN, OUT, 150.0));
        assertNotEquals(record, new BookingRecord(101, "Jane Doe", IN, OUT, 150.0));
        assertNotEquals(record, new BookingRecord(101, "John Doe", IN.minusDays(1), OUT, 150.0));
        assertNotEquals(record, new BookingRecord(101, "John Doe", IN, OUT.plusDays(1), 150.0));
        assertNotEquals(record, new BookingRecord(101, "John Doe", IN, OUT, 151.0));

        Set<BookingRecord> set = new HashSet<>(List.of(record, sample()));
        assertEquals(1, set.size());
    }

    @Test
    void keyIsRoomAndCheckIn() {
        assertTrue(sample().hasKey(101, IN));
        assertFalse(sample().hasKey(102, IN));
        assertFalse(sample().hasKey(101, OUT));
    }

    // ---------- Розбір CSV-полів ----------

    @Test
    void fromFieldsRestoresRecordInColumnOrder() throws DataStorageException {
        assertEquals(sample(),
                BookingRecord.fromFields(List.of("2026-01-01", "2026-01-04", "John Doe", "150.0", "101")));
    }

    @Test
    void wrongFieldCountIsFormatError() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("2026-01-01", "2026-01-04", "John Doe", "150.0")));
        assertEquals("Очікується 5 полів, отримано 4", exception.getMessage());
        assertNull(exception.getCause());
        assertThrows(DataStorageException.class, () -> BookingRecord.fromFields(null));
        assertThrows(DataStorageException.class, () -> BookingRecord.fromFields(
                List.of("2026-01-01", "2026-01-04", "John Doe", "150.0", "101", "extra")));
    }

    @Test
    void invalidNumbersKeepNumberFormatCause() {
        DataStorageException rate = assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("2026-01-01", "2026-01-04", "John Doe", "abc", "101")));
        assertTrue(rate.getMessage().contains("тариф"));
        assertInstanceOf(NumberFormatException.class, rate.getCause());

        DataStorageException room = assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("2026-01-01", "2026-01-04", "John Doe", "150.0", "1x")));
        assertTrue(room.getMessage().contains("номер"));
        assertInstanceOf(NumberFormatException.class, room.getCause());
    }

    @Test
    void invalidDateKeepsParseCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("01.01.2026", "2026-01-04", "John Doe", "150.0", "101")));
        assertTrue(exception.getMessage().contains("заїзд"));
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    void modelViolationKeepsIllegalArgumentCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("2026-01-04", "2026-01-01", "John Doe", "150.0", "101")));
        assertTrue(exception.getMessage().contains("Check-out must be after check-in"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("2026-01-01", "2026-01-04", "", "150.0", "101")));
        assertThrows(DataStorageException.class,
                () -> BookingRecord.fromFields(List.of("2026-01-01", "2026-01-04", "John Doe", "-5", "101")));
    }
}
