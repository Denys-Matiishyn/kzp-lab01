package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void fromCsvCreatesValidEntity() {
        Booking booking = Booking.fromCsv("101;John Doe;3;150.00;Standard");

        assertEquals(101, booking.getRoom());
        assertEquals("John Doe", booking.getGuest());
        assertEquals(3, booking.getNights());
        assertEquals(150.00, booking.getNightlyRate(), 0.0001);
        assertEquals("Standard", booking.getCategory());
        assertEquals(450.00, booking.getRevenue(), 0.0001);
    }

    @Test
    void constructorRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Booking(-101, "Jane Doe", 5, 100.0, "Deluxe"));

        assertThrows(IllegalArgumentException.class,
                () -> new Booking(102, "Jane Doe", -5, 100.0, "Deluxe"));
    }

    @Test
    void fromCsvRejectsInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> Booking.fromCsv("101;John Doe;3;150.00")); // лише 4 поля

        assertThrows(IllegalArgumentException.class,
                () -> Booking.fromCsv(";John Doe;3;150.00;Standard")); // порожня кімната
    }

    @Test
    void recordUsesValueEquality() {
        NightsRate first = new NightsRate(5, 15, 1500.0, 7);
        NightsRate second = new NightsRate(5, 15, 1500.0, 7);
        assertEquals(first, second);
    }
}