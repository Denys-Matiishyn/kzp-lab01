package ua.lpnu.kzp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Точка входу консольного застосунку обробки бронювань готелю.
 */
public class Main {
    private static final String VERSION = "3.0.0";

    /** Створює об'єкт точки входу (потрібен лише JVM; статичний метод {@code main} не потребує екземпляра). */
    public Main() {
    }

    /**
     * Читає вхідний CSV, обчислює показники та записує звіт.
     *
     * @param args параметри командного рядка: --help, --version, --input, --output
     */
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
                    System.out.println(VERSION);
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
        // Додатковий блок лише в консолі: файл звіту залишається сумісним із попередньою роботою.
        System.out.print(buildPolymorphicDetails(bookings));

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

    /**
     * Формує додатковий блок із поліморфними показниками. Метод працює зі спільним
     * типом {@link Booking} і не перевіряє конкретний підтип: вартість і назву рівня
     * надає сам об'єкт.
     *
     * @param bookings коректні бронювання різних підтипів
     * @return текст блоку
     */
    static String buildPolymorphicDetails(List<? extends Booking> bookings) {
        double totalStayCost = 0.0;
        Map<String, Integer> perTier = new TreeMap<>();
        for (Booking b : bookings) {
            totalStayCost += b.stayCost();
            perTier.merge(b.tierLabel(), 1, Integer::sum);
        }
        Set<Booking> unique = new HashSet<>(bookings);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%n--- Polymorphic details ---%n"));
        for (Map.Entry<String, Integer> entry : perTier.entrySet()) {
            sb.append(String.format(Locale.ROOT, "%s bookings: %d%n", entry.getKey(), entry.getValue()));
        }
        sb.append(String.format(Locale.ROOT, "Unique bookings: %d%n", unique.size()));
        sb.append(String.format(Locale.ROOT, "Total stay cost (tier rules): $%.2f%n", totalStayCost));
        return sb.toString();
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