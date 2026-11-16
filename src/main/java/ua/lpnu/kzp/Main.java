package ua.lpnu.kzp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Точка входу консольного застосунку обробки бронювань готелю.
 */
public class Main {
    private static final String VERSION = "4.0.0";
    /** Спільна дата заїзду контрольного набору: у вхідному CSV дат немає. */
    static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    /** Дата за замовчуванням для запиту «активне на дату» (третій день періоду). */
    static final LocalDate DEFAULT_DATE = PERIOD_START.plusDays(2);
    /** Кількість записів у запиті top-N. */
    static final int TOP_LIMIT = 5;

    /** Створює об'єкт точки входу (потрібен лише JVM; статичний метод {@code main} не потребує екземпляра). */
    public Main() {
    }

    /**
     * Читає вхідний CSV, обчислює показники та записує звіт.
     *
     * @param args параметри командного рядка: --help, --version, --input, --output,
     *             --date, --find-room
     */
    public static void main(String[] args) {
        String inputFile = "data/input.csv";
        String outputFile = "out/report.txt";
        LocalDate date = DEFAULT_DATE;
        Integer findRoom = null;

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
                    System.out.println("  --date YYYY-MM-DD Date for the 'active bookings' query (default: "
                            + DEFAULT_DATE + ", check-in " + PERIOD_START + ")");
                    System.out.println("  --find-room N     Search a booking by room number");
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
                case "--date":
                    if (i + 1 < args.length) {
                        try {
                            date = LocalDate.parse(args[++i]);
                        } catch (DateTimeParseException e) {
                            System.out.println("Invalid date (expected YYYY-MM-DD): " + args[i]);
                            return;
                        }
                    }
                    break;
                case "--find-room":
                    if (i + 1 < args.length) {
                        try {
                            findRoom = Integer.valueOf(args[++i]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid room number: " + args[i]);
                            return;
                        }
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

        // Показники звіту обчислюються потоками; формат і значення звіту не змінилися.
        NightsRate summary = BookingQueries.summarize(bookings);
        String summaryText = buildSummary(summary.totalRecords(), summary.totalNights(), summary.totalRevenue(), summary.maxNights());

        System.out.println("\n--- Final Results ---");
        System.out.print(summaryText);
        // Додатковий блок лише в консолі: файл звіту залишається сумісним із попередньою роботою.
        System.out.print(buildPolymorphicDetails(bookings));
        System.out.print(buildStreamQueries(bookings, date, findRoom));

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
    static String buildPolymorphicDetails(List<Booking> bookings) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%n--- Polymorphic details ---%n"));
        BookingQueries.countByTier(bookings).forEach((tier, count) ->
                sb.append(String.format(Locale.ROOT, "%s bookings: %d%n", tier, count)));
        sb.append(String.format(Locale.ROOT, "Unique bookings: %d%n",
                BookingQueries.uniqueCount(bookings)));
        sb.append(String.format(Locale.ROOT, "Total stay cost (tier rules): $%.2f%n",
                BookingQueries.totalStayCost(bookings)));
        return sb.toString();
    }

    /**
     * Формує консольний блок із результатами п'яти запитів Stream API та пошуку.
     * Обчислення виконує {@link BookingQueries}; тут лише форматування тексту.
     *
     * @param bookings коректні бронювання
     * @param date     дата для запиту «активне на дату»
     * @param findRoom номер кімнати для пошуку або {@code null}, якщо пошук не замовлено
     * @return текст блоку
     */
    static String buildStreamQueries(List<Booking> bookings, LocalDate date,
                                     Integer findRoom) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%n--- Stream queries ---%n"));
        sb.append(String.format(Locale.ROOT, "Active on %s (check-in %s): %d%n", date, PERIOD_START,
                BookingQueries.activeOn(bookings, PERIOD_START, date).size()));
        sb.append(String.format(Locale.ROOT, "Room numbers: %s%n",
                BookingQueries.roomNumbers(bookings).stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "))));
        sb.append(String.format(Locale.ROOT, "Bookings by tariff: %s%n",
                BookingQueries.countByTariff(bookings).entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(", "))));

        IntSummaryStatistics stay = BookingQueries.stayStatistics(bookings);
        if (stay.getCount() == 0) {
            sb.append(String.format(Locale.ROOT, "Stay nights: no data%n"));
        } else {
            sb.append(String.format(Locale.ROOT, "Stay nights: count=%d, min=%d, max=%d, avg=%.2f%n",
                    stay.getCount(), stay.getMin(), stay.getMax(), stay.getAverage()));
        }

        sb.append(String.format(Locale.ROOT, "Top %d longest stays:%n", TOP_LIMIT));
        BookingQueries.longestStays(bookings, TOP_LIMIT).stream()
                .map(booking -> String.format(Locale.ROOT, "  room %d, %s, %d nights%n",
                        booking.getRoom(), booking.getGuest(), booking.getNights()))
                .forEach(sb::append);

        if (findRoom != null) {
            Optional<Booking> found = BookingQueries.findByRoom(bookings, findRoom);
            sb.append(String.format(Locale.ROOT, "Search room %d: %s%n", findRoom,
                    found.map(Booking::toString).orElse("not found")));
        }
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