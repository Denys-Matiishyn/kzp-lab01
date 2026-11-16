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

class MainExportTest {
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
    void exportOptionWritesCsvAndReportsRoundTrip(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("export").resolve("bookings.csv");
        Main.main(new String[] {"--input", "data/input-lab05.csv",
            "--output", dir.resolve("r.txt").toString(), "--export", csv.toString()});

        String console = console();
        assertTrue(console.contains("--- CSV export ---"));
        assertTrue(console.contains("Header: заїзд,виїзд,гість,тариф,номер"));
        assertTrue(console.contains("Exported records: 6"));
        assertTrue(console.contains("Restored records: 6"));
        assertTrue(console.contains("Round-trip equal: true"));

        List<String> lines = Files.readString(csv, StandardCharsets.UTF_8).lines().toList();
        assertEquals(7, lines.size());
        assertEquals("заїзд,виїзд,гість,тариф,номер", lines.get(0));
        assertEquals("2026-01-01,2026-01-08,\"O'Neil, \"\"Shaq\"\" Jr.\",220.5,205", lines.get(3));
        assertEquals("2026-01-01,2026-01-06,Марія Коваленко,2000.0,404", lines.get(4));
    }

    @Test
    void jsonOptionWritesSecondFormat(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("bookings.json");
        Main.main(new String[] {"--input", "data/input-lab05.csv",
            "--output", dir.resolve("r.txt").toString(), "--export-json", json.toString()});

        assertTrue(console().contains("JSON saved to:"));
        String text = Files.readString(json, StandardCharsets.UTF_8);
        assertTrue(text.contains("\"гість\": \"O'Neil, \\\"Shaq\\\" Jr.\""));
        assertTrue(text.contains("\"номер\": 404"));
    }

    @Test
    void withoutExportOptionNothingIsExported(@TempDir Path dir) {
        Main.main(new String[] {"--input", "data/input-lab05.csv",
            "--output", dir.resolve("r.txt").toString()});
        assertFalse(console().contains("--- CSV export ---"));
    }

    @Test
    void exportFailureIsReportedWithCause(@TempDir Path dir) {
        // шлях експорту вказує на каталог: помилка файлової системи не приховується
        Main.main(new String[] {"--input", "data/input-lab05.csv",
            "--output", dir.resolve("r.txt").toString(), "--export", dir.toString()});
        String console = console();
        assertTrue(console.contains("Export failed: Не вдалося записати CSV"));
        assertTrue(console.contains("Cause: "));
        assertTrue(console.contains("Report successfully saved to:"));
    }

    @Test
    void reportFileStaysCompatibleWhenExporting(@TempDir Path dir) throws Exception {
        Path output = dir.resolve("report.txt");
        Main.main(new String[] {"--input", "data/input.csv", "--output", output.toString(),
            "--export", dir.resolve("b.csv").toString()});
        assertEquals(Files.readString(Path.of("baseline/report-lab01.txt")).replace("\r\n", "\n"),
                Files.readString(output).replace("\r\n", "\n"));
        assertTrue(console().contains("Exported records: 12"));
    }
}
