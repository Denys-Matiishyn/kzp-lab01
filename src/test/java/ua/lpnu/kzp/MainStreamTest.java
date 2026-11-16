package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainStreamTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    private String console() {
        return outContent.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void consolePrintsAllStreamQueriesForLab04Data(@TempDir Path dir) {
        Main.main(new String[] {"--input", "data/input-lab04.csv",
            "--output", dir.resolve("r.txt").toString(), "--find-room", "404"});

        String console = console();
        assertTrue(console.contains("--- Stream queries ---"));
        assertTrue(console.contains("Active on 2026-01-03 (check-in 2026-01-01): 5"));
        assertTrue(console.contains("Room numbers: 101, 102, 205, 210, 301, 404"));
        assertTrue(console.contains(
                "Bookings by tariff: ECONOMY=1, STANDARD=2, DELUXE=1, BUSINESS=1, SUITE=1, PENTHOUSE=1"));
        assertTrue(console.contains("Stay nights: count=7, min=1, max=10, avg=5.00"));
        assertTrue(console.contains("Top 5 longest stays:\n"
                + "  room 210, Sarah Connor, 10 nights\n"
                + "  room 205, Diana Prince, 7 nights\n"
                + "  room 301, Dexter Morgan, 7 nights\n"
                + "  room 404, Tom Cruise, 5 nights\n"
                + "  room 101, John Doe, 3 nights\n"));
        assertTrue(console.contains("Search room 404: Tom Cruise (Penthouse)"));
    }

    @Test
    void searchForAbsentRoomPrintsNotFound(@TempDir Path dir) {
        Main.main(new String[] {"--input", "data/input-lab04.csv",
            "--output", dir.resolve("r.txt").toString(), "--find-room", "999"});
        assertTrue(console().contains("Search room 999: not found"));
    }

    @Test
    void dateOptionChangesActiveBookings(@TempDir Path dir) {
        Main.main(new String[] {"--input", "data/input-lab04.csv",
            "--output", dir.resolve("r.txt").toString(), "--date", "2026-01-08"});
        // на восьмий день активним лишається тільки проживання на 10 ночей
        assertTrue(console().contains("Active on 2026-01-08 (check-in 2026-01-01): 1"));
        assertFalse(console().contains("Search room"));
    }

    @Test
    void invalidDateAndRoomAreReported() {
        Main.main(new String[] {"--date", "08.01.2026"});
        Main.main(new String[] {"--find-room", "abc"});
        String console = console();
        assertTrue(console.contains("Invalid date (expected YYYY-MM-DD): 08.01.2026"));
        assertTrue(console.contains("Invalid room number: abc"));
    }

    @Test
    void emptyInputFileGivesEmptyQueryResults(@TempDir Path dir) throws Exception {
        Path input = dir.resolve("empty.csv");
        Files.writeString(input, "room;guest;nights;nightlyRate;category\n");
        Main.main(new String[] {"--input", input.toString(),
            "--output", dir.resolve("r.txt").toString(), "--find-room", "101"});

        String console = console();
        assertTrue(console.contains("Total valid records: 0"));
        assertTrue(console.contains("Active on 2026-01-03 (check-in 2026-01-01): 0"));
        assertTrue(console.contains("Room numbers: \n"));
        assertTrue(console.contains("Bookings by tariff: \n"));
        assertTrue(console.contains("Stay nights: no data"));
        assertTrue(console.contains("Search room 101: not found"));
    }

    @Test
    void streamBlockIsNotWrittenToReportFile(@TempDir Path dir) throws Exception {
        Path output = dir.resolve("report.txt");
        Main.main(new String[] {"--input", "data/input-lab04.csv", "--output", output.toString()});

        String report = Files.readString(output).replace("\r\n", "\n");
        assertEquals("--- Hotel Data Report ---\n"
                + "Total valid records: 7\n"
                + "Total nights stayed: 35\n"
                + "Total revenue: $21380.00\n"
                + "Maximum nights by a single guest: 10\n", report);
    }

    @Test
    void buildStreamQueriesWithoutSearchOmitsSearchLine() {
        String block = Main.buildStreamQueries(List.of(), Main.DEFAULT_DATE, null);
        assertFalse(block.contains("Search room"));
        assertTrue(block.contains("Top 5 longest stays:"));
    }
}
