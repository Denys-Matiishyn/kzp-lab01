package ua.lpnu.kzp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        String inputFile = "data/input.csv";
        String outputFile = "out/report.txt";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    System.out.println("Hotel Data Processing Tool");
                    System.out.println("Usage: java -jar lab01.jar [options]");
                    System.out.println("Options:");
                    System.out.println("  --help            Show this help message");
                    System.out.println("  --version         Show version information");
                    System.out.println("  --input FILE      Specify input CSV file (default: data/input.csv)");
                    System.out.println("  --output FILE     Specify output text file (default: out/report.txt)");
                    return;
                case "--version":
                    System.out.println("1.0.0");
                    return;
                case "--input":
                    if (i + 1 < args.length) {
                        inputFile = args[++i];
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    }
                    break;
                default:
                    break;
            }
        }

        List<Booking> bookings = new ArrayList<>();
        System.out.println("--- Hotel Data Processing ---");

        try (BufferedReader br = Files.newBufferedReader(Path.of(inputFile), StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (lineNumber == 1 && line.toLowerCase(Locale.ROOT).contains("guest")) {
                    continue;
                }

                if (line.trim().isEmpty()) {
                    System.out.println("Row " + lineNumber + ": Порожній рядок");
                    continue;
                }

                try {
                    Booking booking = Booking.fromCsv(line);
                    bookings.add(booking);
                } catch (IllegalArgumentException e) {
                    System.out.println("Row " + lineNumber + ": " + e.getMessage() + " -> " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading input file: " + e.getMessage());
            return;
        }

        int totalNights = 0;
        double totalRevenue = 0.0;
        int maxNights = 0;

        for (Booking b : bookings) {
            totalNights += b.getNights();
            totalRevenue += b.getRevenue();
            if (b.getNights() > maxNights) {
                maxNights = b.getNights();
            }
        }

        NightsRate summary = new NightsRate(bookings.size(), totalNights, totalRevenue, maxNights);
        String summaryText = buildSummary(summary.totalRecords(), summary.totalNights(), summary.totalRevenue(), summary.maxNights());

        System.out.println("\n--- Final Results ---");
        System.out.print(summaryText);

        try {
            Path outPath = Path.of(outputFile);
            Path parent = outPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String fileContent = "--- Hotel Data Report ---" + System.lineSeparator() + summaryText;
            Files.writeString(outPath, fileContent, StandardCharsets.UTF_8);
            System.out.println("\nReport successfully saved to: " + outputFile);
        } catch (IOException e) {
            System.out.println("Error writing to output file: " + e.getMessage());
        }
    }

    private static String buildSummary(int totalRecords, int totalNights, double totalRevenue, int maxNights) {
        return String.format(Locale.ROOT,
                "Total valid records: %d%n"
                        + "Total nights stayed: %d%n"
                        + "Total revenue: $%.2f%n"
                        + "Maximum nights by a single guest: %d%n",
                totalRecords, totalNights, totalRevenue, maxNights);
    }
}