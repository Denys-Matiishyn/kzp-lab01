package ua.lpnu.kzp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;

/**
 * Main class for Hotel Data Processing.
 * Reads customer records from a CSV file, validates them,
 * and generates a summary report including total revenue and maximum nights.
 */
public class Main {

    /**
     * Точка входу в програму.
     * Обробляє аргументи командного рядка, зчитує вхідні дані,
     * перевіряє їх на коректність та виводить статистику.
     *
     * @param args масив параметрів командного рядка
     */
    public static void main(String[] args) {
        String inputFile = "data/input.csv";
        String outputFile = "out/report.txt";

        // Обробка аргументів командного рядка
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

        int totalRecords = 0;
        int totalNights = 0;
        double totalRevenue = 0.0;
        int maxNights = 0;

        System.out.println("--- Hotel Data Processing ---");

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Пропускаємо заголовок (перевіряємо на наявність слова 'guest')
                if (isFirstLine && line.toLowerCase().contains("guest")) {
                    isFirstLine = false;
                    continue;
                }

                // Виправлення: Порожній рядок тепер відкидається з виведенням причини
                if (line.trim().isEmpty()) {
                    System.out.println("Row " + lineNumber + ": Порожній рядок");
                    continue;
                }

                String[] parts = line.split(";",-1);
                if (parts.length != 5) {
                    System.out.println("Row " + lineNumber + ": Invalid format (expected 5 columns) -> " + line);
                    continue;
                }

                try {
                    // Виправлення: Правильний порядок полів (room;guest;nights;nightlyRate;category)
                    String roomStr = parts[0].trim();
                    String guest = parts[1].trim();
                    String nightsStr = parts[2].trim();
                    String rateStr = parts[3].trim();
                    String category = parts[4].trim();

                    // Перевірки порожніх текстових полів
                    if (roomStr.isEmpty()) {
                        System.out.println("Row " + lineNumber + ": Room number is missing -> " + line);
                        continue;
                    }
                    if (guest.isEmpty()) {
                        System.out.println("Row " + lineNumber + ": Guest name is missing -> " + line);
                        continue;
                    }
                    if (category.isEmpty()) {
                        System.out.println("Row " + lineNumber + ": Category is missing -> " + line);
                        continue;
                    }

                    // Парсинг числових полів
                    int room = Integer.parseInt(roomStr);
                    int nights = Integer.parseInt(nightsStr);
                    double nightlyRate = Double.parseDouble(rateStr);

                    // Виправлення: Жорстка валідація чисел (від'ємні, NaN, Infinity) ДО оновлення статистики
                    if (room <= 0) {
                        System.out.println("Row " + lineNumber + ": Invalid room number (must be > 0) -> " + line);
                        continue;
                    }
                    if (nights <= 0) {
                        System.out.println("Row " + lineNumber + ": Invalid nights count (must be > 0) -> " + line);
                        continue;
                    }
                    if (nightlyRate <= 0.0 || Double.isNaN(nightlyRate) || Double.isInfinite(nightlyRate)) {
                        System.out.println("Row " + lineNumber + ": Invalid nightly rate (must be a valid positive number) -> " + line);
                        continue;
                    }

                    // Якщо всі перевірки пройдено, оновлюємо статистику
                    totalRecords++;
                    totalNights += nights;
                    totalRevenue += (nights * nightlyRate);
                    if (nights > maxNights) {
                        maxNights = nights;
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Row " + lineNumber + ": Number parsing error (check ints/doubles) -> " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading input file: " + e.getMessage());
            return;
        }

        System.out.println("\n--- Final Results ---");
        System.out.println("Total valid records: " + totalRecords);
        System.out.println("Total nights stayed: " + totalNights);
        System.out.println("Total revenue: $" + String.format(Locale.US, "%.2f", totalRevenue));
        System.out.println("Maximum nights by a single guest: " + maxNights);

        try {
            Files.createDirectories(Paths.get("out"));
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
                bw.write("--- Hotel Data Report ---\n");
                bw.write("Total valid records: " + totalRecords + "\n");
                bw.write("Total nights stayed: " + totalNights + "\n");
                bw.write("Total revenue: $" + String.format(Locale.US, "%.2f", totalRevenue) + "\n");
                bw.write("Maximum nights by a single guest: " + maxNights + "\n");
            }
            System.out.println("\nReport successfully saved to: " + outputFile);
        } catch (IOException e) {
            System.out.println("Error writing to output file: " + e.getMessage());
        }
    }
}