package ua.lpnu.kzp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        String inputFile = "data/input.csv";
        String outputFile = "out/report.txt";

        int totalRecords = 0;
        int totalNights = 0;
        double totalRevenue = 0.0;
        int maxNights = 0;

        System.out.println("--- Hotel Data Processing ---");

        // Виправлення SpotBugs: чітко вказано кодування UTF-8
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Пропускаємо заголовок, якщо він є
                if (isFirstLine && line.toLowerCase().contains("guest")) {
                    isFirstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length != 5) {
                    System.out.println("Row " + lineNumber + ": Invalid format (expected 5 columns) -> " + line);
                    continue;
                }

                try {
                    String guest = parts[0].trim();
                    int room = Integer.parseInt(parts[1].trim());
                    int nights = Integer.parseInt(parts[2].trim());
                    double nightlyRate = Double.parseDouble(parts[3].trim());
                    String category = parts[4].trim();

                    if (guest.isEmpty()) {
                        System.out.println("Row " + lineNumber + ": Guest name is missing -> " + line);
                        continue;
                    }

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

        // Вивід результатів у консоль
        System.out.println("\n--- Final Results ---");
        System.out.println("Total valid records: " + totalRecords);
        System.out.println("Total nights stayed: " + totalNights);
        System.out.println("Total revenue: $" + String.format(Locale.US, "%.2f", totalRevenue));
        System.out.println("Maximum nights by a single guest: " + maxNights);

        // Збереження результатів у файл
        try {
            Files.createDirectories(Paths.get("out"));
            // Виправлення SpotBugs: чітко вказано кодування UTF-8
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