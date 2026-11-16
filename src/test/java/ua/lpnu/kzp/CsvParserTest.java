package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void parsesSimpleRecords() throws DataStorageException {
        assertEquals(List.of(List.of("a", "b", "c"), List.of("1", "2", "3")),
                CsvParser.parseDocument("a,b,c\n1,2,3\n"));
    }

    @Test
    void emptyTextGivesNoRecords() throws DataStorageException {
        assertTrue(CsvParser.parseDocument("").isEmpty());
    }

    @Test
    void lastRecordWithoutLineBreakIsKept() throws DataStorageException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), CsvParser.parseDocument("a,b\n1,2"));
    }

    @Test
    void windowsUnixAndOldMacLineBreaksAreAccepted() throws DataStorageException {
        List<List<String>> expected = List.of(List.of("a", "b"), List.of("1", "2"));
        assertEquals(expected, CsvParser.parseDocument("a,b\r\n1,2\r\n"));
        assertEquals(expected, CsvParser.parseDocument("a,b\n1,2\n"));
        assertEquals(expected, CsvParser.parseDocument("a,b\r1,2\r"));
    }

    @Test
    void commaInsideQuotesIsData() throws DataStorageException {
        assertEquals(List.of("2026-01-01", "Smith, John", "101"),
                CsvParser.parseLine("2026-01-01,\"Smith, John\",101"));
        // простий split ділить те саме поле на дві частини
        assertEquals(4, "2026-01-01,\"Smith, John\",101".split(",").length);
    }

    @Test
    void doubledQuoteInsideQuotesIsOneQuote() throws DataStorageException {
        assertEquals(List.of("O'Neil, \"Shaq\" Jr.", "205"),
                CsvParser.parseLine("\"O'Neil, \"\"Shaq\"\" Jr.\",205"));
    }

    @Test
    void lineBreakInsideQuotesDoesNotSplitRecord() throws DataStorageException {
        List<List<String>> records = CsvParser.parseDocument("h1,h2\n\"line1\nline2\",x\n\"a\r\nb\",y\n");
        assertEquals(3, records.size());
        assertEquals(List.of("line1\nline2", "x"), records.get(1));
        assertEquals(List.of("a\r\nb", "y"), records.get(2));
    }

    @Test
    void emptyFieldsAreKept() throws DataStorageException {
        assertEquals(List.of("", "", ""), CsvParser.parseLine(",,"));
        assertEquals(List.of("a", ""), CsvParser.parseLine("a,"));
        assertEquals(List.of(""), CsvParser.parseLine("\"\""));
    }

    @Test
    void unclosedQuoteIsFormatError() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("a,b\n\"open,2\n"));
        assertTrue(exception.getMessage().contains("Незакриті CSV-лапки"));
        assertTrue(exception.getMessage().contains("записі 2"));
    }

    @Test
    void textAfterClosedQuoteIsFormatError() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"a\"b,c\n"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"a\"\"\"b\n"));
    }

    @Test
    void quoteInsideUnquotedFieldIsFormatError() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("ab\"c,d\n"));
    }

    @Test
    void nullDocumentAndWrongRecordCountAreRejected() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument(null));
        assertThrows(DataStorageException.class, () -> CsvParser.parseLine(""));
        assertThrows(DataStorageException.class, () -> CsvParser.parseLine("a\nb\n"));
    }

    @Test
    void parserReversesExporterEscaping() throws DataStorageException {
        for (String text : List.of("plain", "a,b", "say \"hi\"", "two\nlines", "win\r\nbreak", "", " spaced ")) {
            assertEquals(List.of(text, "end"),
                    CsvParser.parseLine(CsvExporter.escape(text) + ",end"), text);
        }
    }
}
