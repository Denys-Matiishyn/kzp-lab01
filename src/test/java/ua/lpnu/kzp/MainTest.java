package ua.lpnu.kzp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testVersionArgument() {
        Main.main(new String[]{"--version"});
        assertTrue(outContent.toString().contains("1.0.0"));
    }

    @Test
    public void testHelpArgument() {
        Main.main(new String[]{"--help"});
        assertTrue(outContent.toString().contains("Usage:"));
    }

    @Test
    public void testEmptyFile() throws IOException {
        Path tempFile = Files.createTempFile("empty", ".csv");
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Total valid records: 0"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testValidRecord() throws IOException {
        Path tempFile = Files.createTempFile("valid", ".csv");
        Files.writeString(tempFile, "guest;room;nights;nightlyRate;category\nJohn;101;2;100.0;Standard");
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Total revenue: $200.00"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testMissingGuestName() throws IOException {
        Path tempFile = Files.createTempFile("missing", ".csv");
        Files.writeString(tempFile, ";101;2;100.0;Standard");
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Guest name is missing"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testInvalidColumnCount() throws IOException {
        Path tempFile = Files.createTempFile("cols", ".csv");
        Files.writeString(tempFile, "John;101;2;100.0"); // 4 columns
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Invalid format (expected 5 columns)"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testNonNumericValue() throws IOException {
        Path tempFile = Files.createTempFile("num", ".csv");
        Files.writeString(tempFile, "John;101;two;100.0;Standard");
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Number parsing error"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testMultipleRecordsMaxNights() throws IOException {
        Path tempFile = Files.createTempFile("multi", ".csv");
        Files.writeString(tempFile, "A;1;2;10;S\nB;2;5;10;S\nC;3;1;10;S");
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Maximum nights by a single guest: 5"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testUtf8UkrainianText() throws IOException {
        Path tempFile = Files.createTempFile("utf8", ".csv");
        Files.writeString(tempFile, "Денис;101;3;150.0;Люкс");
        Main.main(new String[]{"--input", tempFile.toString()});
        assertTrue(outContent.toString().contains("Total revenue: $450.00"));
        Files.deleteIfExists(tempFile);
    }
}