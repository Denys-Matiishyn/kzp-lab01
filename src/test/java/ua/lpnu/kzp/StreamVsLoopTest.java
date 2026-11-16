package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.IntSummaryStatistics;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Порівнює потокові запити з еталонною цикловою реалізацією на контрольних наборах.
 */
class StreamVsLoopTest {
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 1, 1);
    private static final List<String> FILES = List.of("data/input.csv", "data/input-lab04.csv");

    private static List<Booking> load(String file) throws IOException {
        return Files.readAllLines(Path.of(file), StandardCharsets.UTF_8).stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(Booking::fromCsv)
                .toList();
    }

    @Test
    void filterMatchesLoopForEveryDayOfPeriod() throws IOException {
        for (String file : FILES) {
            List<Booking> bookings = load(file);
            for (int day = -1; day <= 11; day++) {
                LocalDate date = CHECK_IN.plusDays(day);
                assertEquals(LoopReference.activeOn(bookings, CHECK_IN, date),
                        BookingQueries.activeOn(bookings, CHECK_IN, date), file + " day " + day);
            }
        }
    }

    @Test
    void mapAndGroupingMatchLoop() throws IOException {
        for (String file : FILES) {
            List<Booking> bookings = load(file);
            assertEquals(LoopReference.roomNumbers(bookings), BookingQueries.roomNumbers(bookings), file);
            assertEquals(LoopReference.countByTariff(bookings), BookingQueries.countByTariff(bookings), file);
        }
    }

    @Test
    void statisticsMatchLoop() throws IOException {
        for (String file : FILES) {
            List<Booking> bookings = load(file);
            long[] expected = LoopReference.stayStatistics(bookings);
            IntSummaryStatistics actual = BookingQueries.stayStatistics(bookings);
            assertEquals(expected[0], actual.getCount(), file);
            assertEquals(expected[1], actual.getMin(), file);
            assertEquals(expected[2], actual.getMax(), file);
            assertEquals(expected[3], actual.getSum(), file);
        }
    }

    @Test
    void topNMatchesLoopForEveryLimit() throws IOException {
        for (String file : FILES) {
            List<Booking> bookings = load(file);
            for (int limit = 0; limit <= bookings.size() + 2; limit++) {
                assertEquals(LoopReference.longestStays(bookings, limit),
                        BookingQueries.longestStays(bookings, limit), file + " limit " + limit);
            }
        }
    }

    @Test
    void searchMatchesLoopForPresentAndAbsentRooms() throws IOException {
        for (String file : FILES) {
            List<Booking> bookings = load(file);
            for (int room : new int[] {101, 102, 205, 404, 500, 999}) {
                Booking expected = LoopReference.findByRoom(bookings, room);
                Booking actual = BookingQueries.findByRoom(bookings, room).orElse(null);
                if (expected == null) {
                    assertNull(actual, file + " room " + room);
                } else {
                    assertEquals(expected, actual, file + " room " + room);
                }
            }
        }
    }

    @Test
    void reportSummaryMatchesLoop() throws IOException {
        for (String file : FILES) {
            List<Booking> bookings = load(file);
            NightsRate expected = LoopReference.summarize(bookings);
            NightsRate actual = BookingQueries.summarize(bookings);
            assertEquals(expected.totalRecords(), actual.totalRecords(), file);
            assertEquals(expected.totalNights(), actual.totalNights(), file);
            assertEquals(expected.maxNights(), actual.maxNights(), file);
            assertEquals(expected.totalRevenue(), actual.totalRevenue(), 0.000001, file);
        }
    }
}
