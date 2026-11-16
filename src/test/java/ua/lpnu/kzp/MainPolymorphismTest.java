package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainPolymorphismTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }

    @Test
    void reportFileIsIdenticalToBaselineOfPreviousLab(@TempDir Path dir) throws Exception {
        Path output = dir.resolve("report.txt");
        Main.main(new String[] {"--input", "data/input.csv", "--output", output.toString()});

        String expected = normalize(Files.readString(Path.of("baseline/report-lab01.txt")));
        String actual = normalize(Files.readString(output));
        assertEquals(expected, actual);
    }

    @Test
    void consolePrintsPolymorphicDetailsForMixedData(@TempDir Path dir) throws Exception {
        Path output = dir.resolve("report.txt");
        Main.main(new String[] {"--input", "data/input-lab03.csv", "--output", output.toString()});

        String console = normalize(outContent.toString());
        // два хибні рядки відхилено, чотири коректні різних підтипів прийнято
        assertTrue(console.contains("Row 6: Invalid nights count"));
        assertTrue(console.contains("Row 7: Category is missing"));
        assertTrue(console.contains("Total valid records: 4"));
        assertTrue(console.contains("Standard bookings: 2"));
        assertTrue(console.contains("Premium bookings: 2"));
        assertTrue(console.contains("Unique bookings: 4"));
        // 450 + 3300 (люкс +10%) + 1520 (8 ночей, знижка 5%) + 11000 (пентхаус +10%)
        assertTrue(console.contains("Total stay cost (tier rules): $16270.00"));
        // базовий виторг у файлі звіту не залежить від підтипів
        String report = normalize(Files.readString(output));
        assertTrue(report.contains("Total revenue: $15050.00"));
        assertTrue(!report.contains("Polymorphic"));
    }

    @Test
    void polymorphicDetailsCountDuplicatesOnlyOnce() {
        List<Booking> bookings = List.of(
                Booking.fromCsv("101;John Doe;3;150.00;Standard"),
                Booking.fromCsv("101;John Doe;3;150.00;Standard"),
                Booking.fromCsv("301;Dexter Morgan;3;1000.00;Suite"));
        String details = normalize(Main.buildPolymorphicDetails(bookings));
        assertTrue(details.contains("Standard bookings: 2"));
        assertTrue(details.contains("Premium bookings: 1"));
        assertTrue(details.contains("Unique bookings: 2"));
    }

    @Test
    void emptyListGivesZeroCost() {
        String details = normalize(Main.buildPolymorphicDetails(List.of()));
        assertTrue(details.contains("Unique bookings: 0"));
        assertTrue(details.contains("Total stay cost (tier rules): $0.00"));
    }

    @Test
    void versionOptionPrintsCurrentVersion() {
        Main.main(new String[] {"--version"});
        assertEquals("5.0.0", normalize(outContent.toString()).trim());
    }
}
