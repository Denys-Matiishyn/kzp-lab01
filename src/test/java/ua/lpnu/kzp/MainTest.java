package ua.lpnu.kzp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
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
    void testValidDataProcess() throws Exception {
        Path tempInput = Files.createTempFile("test_input", ".csv");
        Files.writeString(tempInput, "room;guest;nights;nightlyRate;category\n101;John Doe;3;50.0;Standard\n102;Jane Doe;5;100.0;Deluxe");

        String[] args = {"--input", tempInput.toString()};
        Main.main(args);

        String output = outContent.toString();
        assertTrue(output.contains("Total valid records: 2"));
        assertTrue(output.contains("Total nights stayed: 8"));
        assertTrue(output.contains("Total revenue: $650.00"));
        assertTrue(output.contains("Maximum nights by a single guest: 5"));

        Files.deleteIfExists(tempInput);
    }

    @Test
    void testEmptyAndInvalidRows() throws Exception {
        Path tempInput = Files.createTempFile("test_input_invalid", ".csv");
        String data = "room;guest;nights;nightlyRate;category\n" +
                "\n" + // Порожній рядок
                "101;;3;50.0;Standard\n" + // Порожній guest
                "102;John Doe;5;100.0;\n" + // Порожня category
                ";Jane Doe;2;80.0;Standard\n" + // Порожня room
                "John Doe;101;3;50.0;Standard\n"; // Неправильний порядок полів
        Files.writeString(tempInput, data);

        String[] args = {"--input", tempInput.toString()};
        Main.main(args);

        String output = outContent.toString();
        assertTrue(output.contains("Порожній рядок"));
        assertTrue(output.contains("Guest name is missing"));
        assertTrue(output.contains("Category is missing"));
        assertTrue(output.contains("Room number is missing"));
        assertTrue(output.contains("Number parsing error"));
        assertTrue(output.contains("Total valid records: 0"));

        Files.deleteIfExists(tempInput);
    }

    @Test
    void testNegativeAndNaNValues() throws Exception {
        Path tempInput = Files.createTempFile("test_input_math", ".csv");
        String data = "room;guest;nights;nightlyRate;category\n" +
                "-101;John Doe;3;50.0;Standard\n" + // Від'ємна кімната
                "102;Jane Doe;-5;100.0;Deluxe\n" + // Від'ємні ночі
                "103;Bob;2;-50.0;Standard\n" + // Від'ємна ціна
                "104;Alice;4;NaN;Deluxe\n" + // NaN ціна
                "105;Eve;1;Infinity;Standard\n"; // Infinity ціна
        Files.writeString(tempInput, data);

        String[] args = {"--input", tempInput.toString()};
        Main.main(args);

        String output = outContent.toString();
        assertTrue(output.contains("Invalid room number (must be > 0)"));
        assertTrue(output.contains("Invalid nights count (must be > 0)"));
        assertTrue(output.contains("Invalid nightly rate"));
        assertTrue(output.contains("Total valid records: 0"));

        Files.deleteIfExists(tempInput);
    }

    @Test
    void testOutputParameter() throws Exception {
        Path tempInput = Files.createTempFile("test_input_out", ".csv");
        Files.writeString(tempInput, "101;John;2;50.0;Standard");

        Path tempOutput = Files.createTempFile("test_output", ".txt");

        String[] args = {"--input", tempInput.toString(), "--output", tempOutput.toString()};
        Main.main(args);

        String outputContent = Files.readString(tempOutput);
        assertTrue(outputContent.contains("Total valid records: 1"));
        assertTrue(outputContent.contains("Total nights stayed: 2"));
        assertTrue(outputContent.contains("Total revenue: $100.00"));

        Files.deleteIfExists(tempInput);
        Files.deleteIfExists(tempOutput);
    }
}