package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BookingCsvRoundTripTest {
    private static final LocalDate JAN1 = LocalDate.of(2026, 1, 1);

    /** Шість записів: кома й лапки, кирилиця, перенесення рядка, дробовий тариф, повтор номера. */
    private static List<BookingRecord> sample() {
        return List.of(
                new BookingRecord(101, "John Doe", JAN1, JAN1.plusDays(3), 150.0),
                new BookingRecord(301, "Dexter Morgan", JAN1, JAN1.plusDays(7), 1000.0),
                new BookingRecord(205, "O'Neil, \"Shaq\" Jr.", JAN1, JAN1.plusDays(7), 220.5),
                new BookingRecord(404, "Марія Коваленко", LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 2), 2000.0),
                new BookingRecord(210, "Sarah\nConnor", JAN1, JAN1.plusDays(10), 199.99),
                new BookingRecord(101, "Anna Bell", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), 150.0));
    }

    // ---------- Round-trip ----------

    @Test
    void roundTripThroughFileRestoresEqualObjects(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bookings.csv");
        List<BookingRecord> original = sample();

        CsvExporter.write(file, BookingRecord.class, original);
        List<BookingRecord> restored = BookingCsvReader.read(file);

        assertEquals(original, restored);
        assertEquals(6, restored.size());
        // 1 рядок заголовка + 6 записів, один із яких займає два фізичні рядки
        assertEquals(8, Files.readString(file, StandardCharsets.UTF_8).lines().count());
        assertEquals(7, CsvParser.parseDocument(Files.readString(file, StandardCharsets.UTF_8)).size());
    }

    @Test
    void exportedTextContainsHeaderAndEscapedRow() throws DataStorageException {
        String csv = CsvExporter.toCsv(BookingRecord.class, sample());
        List<String> lines = csv.lines().toList();
        assertEquals("заїзд,виїзд,гість,тариф,номер", lines.get(0));
        assertEquals("2026-01-01,2026-01-04,John Doe,150.0,101", lines.get(1));
        assertEquals("2026-01-01,2026-01-08,\"O'Neil, \"\"Shaq\"\" Jr.\",220.5,205", lines.get(3));
        assertEquals("2026-02-28,2026-03-02,Марія Коваленко,2000.0,404", lines.get(4));
    }

    @Test
    void restoredRecordIsFoundByKeyRoomAndCheckIn(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bookings.csv");
        CsvExporter.write(file, BookingRecord.class, sample());

        Repository<BookingRecord> repository = new Repository<>();
        BookingCsvReader.read(file).forEach(repository::add);

        // номер 101 трапляється двічі; ключ room + checkIn розрізняє записи
        assertEquals(2, repository.find(record -> record.getRoom() == 101).size());
        List<BookingRecord> found = repository.find(record -> record.hasKey(101, LocalDate.of(2026, 1, 10)));
        assertEquals(1, found.size());
        assertEquals("Anna Bell", found.get(0).getGuest());
        assertTrue(repository.find(record -> record.hasKey(999, JAN1)).isEmpty());
    }

    @Test
    void multilineGuestSurvivesRoundTrip() throws DataStorageException {
        List<BookingRecord> restored = BookingCsvReader.parse(CsvExporter.toCsv(BookingRecord.class, sample()));
        assertEquals("Sarah\nConnor", restored.get(4).getGuest());
    }

    @Test
    void textWithWindowsAndUnixLineBreaksGivesSameRecords() throws DataStorageException {
        String unix = "заїзд,виїзд,гість,тариф,номер\n2026-01-01,2026-01-04,John Doe,150.0,101\n";
        String windows = unix.replace("\n", "\r\n");
        List<BookingRecord> expected = List.of(new BookingRecord(101, "John Doe", JAN1, JAN1.plusDays(3), 150.0));
        assertEquals(expected, BookingCsvReader.parse(unix));
        assertEquals(expected, BookingCsvReader.parse(windows));
    }

    @Test
    void roundTripFromBookingsOfBothSubtypes(@TempDir Path dir) throws Exception {
        List<BookingRecord> original = List.of(
                BookingRecord.fromBooking(Booking.fromCsv("101;John Doe;3;150.00;Standard"), JAN1),
                BookingRecord.fromBooking(Booking.fromCsv("301;Dexter Morgan;7;1000.00;Suite"), JAN1));
        Path file = dir.resolve("b.csv");
        CsvExporter.write(file, BookingRecord.class, original);
        assertEquals(original, BookingCsvReader.read(file));
    }

    // ---------- Порожні дані ----------

    @Test
    void emptyRepositoryRoundTripGivesEmptyList(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("empty.csv");
        CsvExporter.write(file, BookingRecord.class, new Repository<BookingRecord>().all());
        assertEquals("заїзд,виїзд,гість,тариф,номер" + System.lineSeparator(),
                Files.readString(file, StandardCharsets.UTF_8));
        assertTrue(BookingCsvReader.read(file).isEmpty());
    }

    @Test
    void headerOnlyFileAndZeroByteFileAreBothEmpty(@TempDir Path dir) throws Exception {
        assertTrue(BookingCsvReader.read(Path.of("data", "bookings-empty.csv")).isEmpty());
        Path zero = dir.resolve("zero.csv");
        Files.writeString(zero, "", StandardCharsets.UTF_8);
        assertTrue(BookingCsvReader.read(zero).isEmpty());
    }

    // ---------- Хибні дані ----------

    @Test
    void brokenFileReportsRecordNumberAndKeepsCauseChain() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingCsvReader.read(Path.of("data", "bookings-broken.csv")));
        assertTrue(exception.getMessage().startsWith("CSV-запис 2:"));
        assertTrue(exception.getMessage().contains("тариф"));
        // ланцюг: запис файла -> поле -> NumberFormatException
        assertInstanceOf(DataStorageException.class, exception.getCause());
        assertInstanceOf(NumberFormatException.class, exception.getCause().getCause());
    }

    @Test
    void wrongFieldCountInFileIsReported() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingCsvReader.parse("заїзд,виїзд,гість,тариф,номер\n2026-01-01,2026-01-04,John Doe,150.0\n"));
        assertEquals("CSV-запис 1: Очікується 5 полів, отримано 4", exception.getMessage());
    }

    @Test
    void unquotedCommaInGuestBreaksFieldCount() {
        // без лапок кома в імені дає шість полів, тому експортер екранує таке значення
        assertThrows(DataStorageException.class,
                () -> BookingCsvReader.parse("заїзд,виїзд,гість,тариф,номер\n2026-01-01,2026-01-04,Smith, John,150.0,101\n"));
    }

    @Test
    void unknownHeaderIsRejected() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingCsvReader.parse("room,guest\n101,John\n"));
        assertTrue(exception.getMessage().contains("Невідомий заголовок CSV"));
    }

    @Test
    void unclosedQuoteInFileIsReported() {
        assertThrows(DataStorageException.class,
                () -> BookingCsvReader.parse("заїзд,виїзд,гість,тариф,номер\n2026-01-01,2026-01-04,\"John,150.0,101\n"));
    }

    // ---------- Файлові помилки ----------

    @Test
    void missingFileGivesStorageExceptionWithIoCause(@TempDir Path dir) {
        Path missing = dir.resolve("no-such-file.csv");
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> BookingCsvReader.read(missing));
        assertTrue(exception.getMessage().startsWith("Не вдалося прочитати CSV"));
        assertNotNull(exception.getCause());
        assertInstanceOf(NoSuchFileException.class, exception.getCause());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void nullPathIsRejected() {
        assertThrows(DataStorageException.class, () -> BookingCsvReader.read(null));
    }
}
