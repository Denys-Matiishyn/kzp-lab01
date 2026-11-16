package ua.lpnu.kzp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Запити Stream API до колекції бронювань.
 *
 * <p>Кожен метод є окремим чистим запитом: створює власний потік, не змінює вхідну
 * колекцію, не форматує текст і не перевіряє конкретний підтип {@link Booking}.
 * Вхідний CSV не містить дат, тому для запиту «активне на дату» заїзд задається
 * параметром, а виїзд обчислюється як заїзд плюс кількість ночей.
 */
public final class BookingQueries {
    /* Забороняє створення екземплярів службового класу. */
    private BookingQueries() {
    }

    /**
     * Запит 1 (filter): бронювання, активні на задану дату.
     * Бронювання активне, якщо {@code checkIn <= date < checkIn + nights}:
     * остання ніч ще враховується, день виїзду вже ні.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @param checkIn  спільна дата заїзду для записів набору
     * @param date     дата, на яку перевіряється активність
     * @return новий незмінний список активних бронювань у порядку вхідної колекції
     */
    public static List<Booking> activeOn(Collection<Booking> bookings,
                                         LocalDate checkIn, LocalDate date) {
        Objects.requireNonNull(bookings, "bookings");
        Objects.requireNonNull(checkIn, "checkIn");
        Objects.requireNonNull(date, "date");
        return bookings.stream()
                .filter(booking -> isActiveOn(booking, checkIn, date))
                .toList();
    }

    /* Перевіряє, що дата потрапляє в проміжок [заїзд; заїзд + ночі). */
    private static boolean isActiveOn(Booking booking, LocalDate checkIn, LocalDate date) {
        LocalDate checkOut = checkIn.plusDays(booking.getNights());
        return !date.isBefore(checkIn) && date.isBefore(checkOut);
    }

    /**
     * Запит 2 (map): номери кімнат без повторів за зростанням.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return новий незмінний список унікальних номерів кімнат
     */
    public static List<Integer> roomNumbers(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream()
                .map(Booking::getRoom)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Запит 3 (groupingBy): кількість бронювань за тарифом.
     * Тарифом вважається тарифна категорія номера {@link RoomKind}.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return карта «тариф → кількість» у порядку оголошення значень enum;
     *         тарифи без бронювань до карти не входять
     */
    public static Map<RoomKind, Long> countByTariff(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream()
                .collect(Collectors.groupingBy(Booking::getKind,
                        () -> new EnumMap<>(RoomKind.class), Collectors.counting()));
    }

    /**
     * Запит 4 (summarizingInt): статистика тривалості проживання в ночах.
     * Для порожнього набору {@code getCount()} дорівнює 0, {@code getAverage()} дорівнює 0.0,
     * а {@code getMin()} і {@code getMax()} мають службові значення
     * {@link Integer#MAX_VALUE} та {@link Integer#MIN_VALUE}, тому перед їх
     * використанням слід перевірити кількість.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return кількість, мінімум, максимум, сума та середнє кількості ночей
     */
    public static IntSummaryStatistics stayStatistics(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream()
                .collect(Collectors.summarizingInt(Booking::getNights));
    }

    /**
     * Складений компаратор для top-N: тривалість проживання за спаданням,
     * за однакової тривалості номер кімнати за зростанням.
     * {@code reversed()} застосовано до першого ключа до {@code thenComparing},
     * тому другий критерій не обертається.
     *
     * @return компаратор «найдовше проживання спочатку»
     */
    public static Comparator<Booking> longestStayFirst() {
        return Comparator.comparingInt(Booking::getNights)
                .reversed()
                .thenComparingInt(Booking::getRoom);
    }

    /**
     * Запит 5 (sorted + limit): перші {@code limit} бронювань із найдовшим проживанням.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @param limit    найбільша кількість записів у результаті (не від'ємна)
     * @return новий незмінний список; порожній для {@code limit == 0},
     *         уся відсортована колекція, якщо {@code limit} перевищує її розмір
     */
    public static List<Booking> longestStays(Collection<Booking> bookings, int limit) {
        Objects.requireNonNull(bookings, "bookings");
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must not be negative");
        }
        return bookings.stream()
                .sorted(longestStayFirst())
                .limit(limit)
                .toList();
    }

    /**
     * Пошук (Optional): перше у порядку колекції бронювання із заданим номером кімнати.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @param room     номер кімнати
     * @return знайдене бронювання або {@link Optional#empty()}, якщо такого номера немає
     */
    public static Optional<Booking> findByRoom(Collection<Booking> bookings, int room) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream()
                .filter(booking -> booking.getRoom() == room)
                .findFirst();
    }

    /**
     * Завдання із зірочкою: дворівневе групування «рівень обслуговування → тариф → кількість».
     * Назву рівня повертає поліморфний метод {@link Booking#tierLabel()}.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return вкладена карта, впорядкована за назвою рівня та порядком значень enum
     */
    public static Map<String, Map<RoomKind, Long>> countByTierAndTariff(
            Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream()
                .collect(Collectors.groupingBy(Booking::tierLabel, TreeMap::new,
                        Collectors.groupingBy(Booking::getKind,
                                () -> new EnumMap<>(RoomKind.class), Collectors.counting())));
    }

    /**
     * Показники звіту попередніх робіт, обчислені потоками замість циклу:
     * кількість записів, сума ночей, загальний виторг і найдовше проживання.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return підсумкові показники; для порожнього набору всі значення нульові
     */
    public static NightsRate summarize(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        int totalNights = bookings.stream().mapToInt(Booking::getNights).sum();
        double totalRevenue = bookings.stream().mapToDouble(Booking::getRevenue).sum();
        int maxNights = bookings.stream().mapToInt(Booking::getNights).max().orElse(0);
        return new NightsRate(bookings.size(), totalNights, totalRevenue, maxNights);
    }

    /**
     * Загальна вартість проживання за правилами підтипів (поліморфний {@link Booking#stayCost()}).
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return сума вартостей; 0.0 для порожнього набору
     */
    public static double totalStayCost(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream().mapToDouble(Booking::stayCost).sum();
    }

    /**
     * Кількість бронювань за рівнем обслуговування.
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return карта «назва рівня → кількість», впорядкована за назвою
     */
    public static Map<String, Long> countByTier(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream()
                .collect(Collectors.groupingBy(Booking::tierLabel, TreeMap::new,
                        Collectors.counting()));
    }

    /**
     * Кількість логічно різних бронювань (за {@code equals}/{@code hashCode}).
     *
     * @param bookings бронювання (колекція не змінюється)
     * @return кількість унікальних бронювань
     */
    public static long uniqueCount(Collection<Booking> bookings) {
        Objects.requireNonNull(bookings, "bookings");
        return bookings.stream().distinct().count();
    }
}
