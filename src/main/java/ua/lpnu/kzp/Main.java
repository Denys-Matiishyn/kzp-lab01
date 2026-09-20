package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        List<String> errors = new ArrayList<>();
        Path inputPath = Path.of("data", "input.csv");

        // Виправлення 1: просто оголошуємо змінну, не створюючи зайвий об'єкт
        List<String> lines;

        try {
            lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Помилка читання файлу. Переконайся, що файл data/input.csv існує.");
            return;
        }

        int validCount = 0;
        int totalNights = 0;
        double totalRevenue = 0.0;
        int maxNights = 0;

        for (int index = 0; index < lines.size(); index++) {
            String[] fields = lines.get(index).split(";", -1);

            if (fields.length != 5) {
                errors.add("Рядок " + (index + 1) + ": очікується 5 полів");
                continue;
            }

            if (fields[0].isBlank() || fields[4].isBlank()) {
                errors.add("Рядок " + (index + 1) + ": порожнє ім'я гостя або категорія номера");
                continue;
            }

            try {
                int room = Integer.parseInt(fields[1].trim());
                int nights = Integer.parseInt(fields[2].trim());
                double nightlyRate = Double.parseDouble(fields[3].trim());

                if (room <= 0 || nights <= 0 || nightlyRate < 0) {
                    errors.add("Рядок " + (index + 1) + ": неприпустимі (від'ємні або нульові) числові значення");
                    continue;
                }

                validCount++;
                totalNights += nights;
                totalRevenue += (nights * nightlyRate);
                maxNights = Math.max(maxNights, nights);

            } catch (NumberFormatException e) {
                errors.add("Рядок " + (index + 1) + ": помилка формату числа");
            }
        }

        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Коректних записів: %d%n", validCount));
        report.append(String.format(Locale.ROOT, "Сумарна кількість ночей: %d%n", totalNights));
        report.append(String.format(Locale.ROOT, "Загальний виторг: %.2f%n", totalRevenue));
        report.append(String.format(Locale.ROOT, "Найдовше проживання (ночей): %d%n", maxNights));

        if (!errors.isEmpty()) {
            report.append(String.format(Locale.ROOT, "%nПомилок знайдено: %d%n", errors.size()));
            for (String error : errors) {
                report.append(error).append(System.lineSeparator());
            }
        }

        String finalReport = report.toString();
        System.out.print(finalReport);

        Path outputPath = Path.of("out", "report.txt");
        try {
            // Виправлення 2: перевіряємо parent на null перед створенням папок
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, finalReport, StandardCharsets.UTF_8);
            System.out.println("\nЗвіт успішно збережено у файл: " + outputPath);
        } catch (IOException e) {
            System.out.println("\nПомилка під час запису файлу звіту.");
        }
    }
}