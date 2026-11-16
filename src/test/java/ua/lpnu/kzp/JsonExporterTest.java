package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonExporterTest {
    private static final LocalDate JAN1 = LocalDate.of(2026, 1, 1);

    @Test
    void jsonUsesSameColumnsAsCsv() throws DataStorageException {
        String json = JsonExporter.toJson(BookingRecord.class,
                List.of(new BookingRecord(101, "John Doe", JAN1, JAN1.plusDays(3), 150.0)));
        assertEquals("[\n  {\"заїзд\": \"2026-01-01\", \"виїзд\": \"2026-01-04\", "
                + "\"гість\": \"John Doe\", \"тариф\": 150.0, \"номер\": 101}\n]", json);
    }

    @Test
    void jsonEscapesQuotesBackslashAndLineBreaks() throws DataStorageException {
        String json = JsonExporter.toJson(BookingRecord.class,
                List.of(new BookingRecord(1, "a\"b\\c\nd\te", JAN1, JAN1.plusDays(1), 1.0)));
        assertTrue(json.contains("\"гість\": \"a\\\"b\\\\c\\nd\\te\""));
    }

    @Test
    void emptyListGivesEmptyArrayAndErrorsAreReported() throws DataStorageException {
        assertEquals("[]", JsonExporter.toJson(BookingRecord.class, List.of()));
        assertThrows(DataStorageException.class, () -> JsonExporter.toJson(BookingRecord.class, null));
        assertThrows(DataStorageException.class, () -> JsonExporter.toJson(String.class, List.of("x")));
    }

    @Test
    void bothFormatsWorkThroughCommonInterface() throws DataStorageException {
        List<BookingRecord> records = List.of(new BookingRecord(101, "John Doe", JAN1, JAN1.plusDays(3), 150.0));
        Exporter<BookingRecord> csv = CsvExporter::toCsv;
        Exporter<BookingRecord> json = JsonExporter::toJson;
        assertTrue(csv.export(BookingRecord.class, records).startsWith("заїзд,виїзд,гість,тариф,номер"));
        assertTrue(json.export(BookingRecord.class, records).startsWith("["));
    }
}
