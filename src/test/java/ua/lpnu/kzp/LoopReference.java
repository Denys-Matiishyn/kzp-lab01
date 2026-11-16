package ua.lpnu.kzp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Еталонна циклова реалізація тих самих запитів. Використовується лише в тестах,
 * щоб довести, що потокові запити дають ті самі результати, що й цикли.
 */
final class LoopReference {
    private LoopReference() {
    }

    static List<Booking> activeOn(List<? extends Booking> bookings, LocalDate checkIn, LocalDate date) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (!date.isBefore(checkIn) && date.isBefore(checkIn.plusDays(b.getNights()))) {
                result.add(b);
            }
        }
        return result;
    }

    static List<Integer> roomNumbers(List<? extends Booking> bookings) {
        TreeSet<Integer> rooms = new TreeSet<>();
        for (Booking b : bookings) {
            rooms.add(b.getRoom());
        }
        return new ArrayList<>(rooms);
    }

    static Map<RoomKind, Long> countByTariff(List<? extends Booking> bookings) {
        Map<RoomKind, Long> result = new EnumMap<>(RoomKind.class);
        for (Booking b : bookings) {
            result.merge(b.getKind(), 1L, Long::sum);
        }
        return result;
    }

    /** Повертає {кількість, мінімум, максимум, сума}; для порожнього входу мінімум і максимум рівні 0. */
    static long[] stayStatistics(List<? extends Booking> bookings) {
        long count = 0;
        long min = 0;
        long max = 0;
        long sum = 0;
        for (Booking b : bookings) {
            int nights = b.getNights();
            if (count == 0 || nights < min) {
                min = nights;
            }
            if (count == 0 || nights > max) {
                max = nights;
            }
            sum += nights;
            count++;
        }
        return new long[] {count, min, max, sum};
    }

    static List<Booking> longestStays(List<? extends Booking> bookings, int limit) {
        List<Booking> sorted = new ArrayList<>(bookings);
        // сортування вставками: ночі за спаданням, далі номер кімнати за зростанням
        for (int i = 1; i < sorted.size(); i++) {
            Booking current = sorted.get(i);
            int j = i - 1;
            while (j >= 0 && isBefore(current, sorted.get(j))) {
                sorted.set(j + 1, sorted.get(j));
                j--;
            }
            sorted.set(j + 1, current);
        }
        List<Booking> result = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < limit; i++) {
            result.add(sorted.get(i));
        }
        return result;
    }

    private static boolean isBefore(Booking a, Booking b) {
        if (a.getNights() != b.getNights()) {
            return a.getNights() > b.getNights();
        }
        return a.getRoom() < b.getRoom();
    }

    static Booking findByRoom(List<? extends Booking> bookings, int room) {
        for (Booking b : bookings) {
            if (b.getRoom() == room) {
                return b;
            }
        }
        return null;
    }

    static NightsRate summarize(List<? extends Booking> bookings) {
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
        return new NightsRate(bookings.size(), totalNights, totalRevenue, maxNights);
    }
}
