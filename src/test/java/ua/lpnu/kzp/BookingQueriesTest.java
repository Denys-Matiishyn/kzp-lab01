package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BookingQueriesTest {
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 1, 1);
    private static final double EPS = 0.0001;

    /** Контрольний набір ЛР № 4: дубль номера 101, рівна тривалість 7 ночей, межа фільтра. */
    private static List<Booking> sample() {
        return List.of(
                Booking.fromCsv("101;John Doe;3;150.00;Standard"),
                Booking.fromCsv("101;Anna Bell;2;150.00;Standard"),
                Booking.fromCsv("301;Dexter Morgan;7;1000.00;Suite"),
                Booking.fromCsv("205;Diana Prince;7;220.00;Deluxe"),
                Booking.fromCsv("404;Tom Cruise;5;2000.00;Penthouse"),
                Booking.fromCsv("210;Sarah Connor;10;200.00;Business"),
                Booking.fromCsv("102;Clark Kent;1;90.00;Economy"));
    }

    private static List<String> guests(List<Booking> bookings) {
        return bookings.stream().map(Booking::getGuest).toList();
    }

    // ---------- Запит 1: filter ----------

    @Test
    void activeOnKeepsBookingsThatCoverTheDate() {
        List<Booking> active = BookingQueries.activeOn(sample(), CHECK_IN, CHECK_IN.plusDays(2));
        assertEquals(List.of("John Doe", "Dexter Morgan", "Diana Prince", "Tom Cruise", "Sarah Connor"),
                guests(active));
    }

    @Test
    void activeOnBoundaryLastNightIsActiveCheckOutDayIsNot() {
        List<Booking> one = List.of(Booking.fromCsv("101;John Doe;3;150.00;Standard"));
        assertEquals(1, BookingQueries.activeOn(one, CHECK_IN, CHECK_IN).size());
        assertEquals(1, BookingQueries.activeOn(one, CHECK_IN, CHECK_IN.plusDays(2)).size());
        assertTrue(BookingQueries.activeOn(one, CHECK_IN, CHECK_IN.plusDays(3)).isEmpty());
    }

    @Test
    void activeOnDateBeforeCheckInGivesEmptyResult() {
        assertTrue(BookingQueries.activeOn(sample(), CHECK_IN, CHECK_IN.minusDays(1)).isEmpty());
    }

    @Test
    void activeOnDateAfterAllStaysGivesEmptyResult() {
        assertTrue(BookingQueries.activeOn(sample(), CHECK_IN, CHECK_IN.plusDays(10)).isEmpty());
    }

    // ---------- Запит 2: map ----------

    @Test
    void roomNumbersAreUniqueAndAscending() {
        assertEquals(List.of(101, 102, 205, 210, 301, 404), BookingQueries.roomNumbers(sample()));
    }

    @Test
    void roomNumbersCollapseDuplicatedRoom() {
        List<Booking> twins = List.of(
                Booking.fromCsv("7;A;1;10.0;Standard"),
                Booking.fromCsv("7;B;2;10.0;Suite"),
                Booking.fromCsv("7;A;1;10.0;Standard"));
        assertEquals(List.of(7), BookingQueries.roomNumbers(twins));
    }

    // ---------- Запит 3: groupingBy ----------

    @Test
    void countByTariffGroupsByRoomKind() {
        Map<RoomKind, Long> counts = BookingQueries.countByTariff(sample());
        assertEquals(6, counts.size());
        assertEquals(2L, counts.get(RoomKind.STANDARD));
        assertEquals(1L, counts.get(RoomKind.SUITE));
        assertEquals(1L, counts.get(RoomKind.DELUXE));
        assertEquals(1L, counts.get(RoomKind.PENTHOUSE));
        assertEquals(1L, counts.get(RoomKind.BUSINESS));
        assertEquals(1L, counts.get(RoomKind.ECONOMY));
        assertFalse(counts.containsKey(RoomKind.PRESIDENTIAL));
    }

    @Test
    void countByTariffKeepsEnumOrderAndSumsToSize() {
        Map<RoomKind, Long> counts = BookingQueries.countByTariff(sample());
        assertEquals(List.of(RoomKind.ECONOMY, RoomKind.STANDARD, RoomKind.DELUXE, RoomKind.BUSINESS,
                RoomKind.SUITE, RoomKind.PENTHOUSE), new ArrayList<>(counts.keySet()));
        assertEquals(sample().size(), counts.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void countByTariffPutsUnknownCategoryIntoOther() {
        Map<RoomKind, Long> counts = BookingQueries.countByTariff(List.of(
                Booking.fromCsv("1;A;1;10.0;Hostel"), Booking.fromCsv("2;B;1;10.0;Capsule")));
        assertEquals(Map.of(RoomKind.OTHER, 2L), counts);
    }

    // ---------- Запит 4: summarizingInt ----------

    @Test
    void stayStatisticsDescribeNights() {
        IntSummaryStatistics stats = BookingQueries.stayStatistics(sample());
        assertEquals(7, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(10, stats.getMax());
        assertEquals(35, stats.getSum());
        assertEquals(5.0, stats.getAverage(), EPS);
    }

    @Test
    void stayStatisticsForSingleBooking() {
        IntSummaryStatistics stats = BookingQueries.stayStatistics(
                List.of(Booking.fromCsv("1;A;4;10.0;Standard")));
        assertEquals(1, stats.getCount());
        assertEquals(4, stats.getMin());
        assertEquals(4, stats.getMax());
        assertEquals(4.0, stats.getAverage(), EPS);
    }

    // ---------- Запит 5: sorted + limit ----------

    @Test
    void longestStaysSortByNightsDescendingThenRoom() {
        List<Booking> top = BookingQueries.longestStays(sample(), 5);
        assertEquals(List.of("Sarah Connor", "Diana Prince", "Dexter Morgan", "Tom Cruise", "John Doe"),
                guests(top));
    }

    @Test
    void longestStaysSecondCriterionOrdersEqualNightsByRoomAscending() {
        List<Booking> top = BookingQueries.longestStays(sample(), 3);
        assertEquals(7, top.get(1).getNights());
        assertEquals(7, top.get(2).getNights());
        assertEquals(205, top.get(1).getRoom());
        assertEquals(301, top.get(2).getRoom());
    }

    @Test
    void longestStaysCutsTheSixthRecord() {
        List<Booking> top = BookingQueries.longestStays(sample(), 5);
        assertEquals(5, top.size());
        assertFalse(guests(top).contains("Anna Bell"));
        assertFalse(guests(top).contains("Clark Kent"));
    }

    @Test
    void longestStaysWithZeroLimitIsEmpty() {
        assertTrue(BookingQueries.longestStays(sample(), 0).isEmpty());
    }

    @Test
    void longestStaysWithTooLargeLimitReturnsAllSorted() {
        List<Booking> all = BookingQueries.longestStays(sample(), 100);
        assertEquals(7, all.size());
        assertEquals("Clark Kent", all.get(6).getGuest());
    }

    @Test
    void longestStaysRejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> BookingQueries.longestStays(sample(), -1));
    }

    @Test
    void comparatorTreatsEqualNightsAndRoomAsEqual() {
        Booking first = Booking.fromCsv("101;John Doe;3;150.00;Standard");
        Booking second = Booking.fromCsv("101;Other Guest;3;999.00;Suite");
        assertEquals(0, BookingQueries.longestStayFirst().compare(first, second));
        assertTrue(BookingQueries.longestStayFirst().compare(
                Booking.fromCsv("9;A;5;1.0;Standard"), Booking.fromCsv("1;B;4;1.0;Standard")) < 0);
    }

    // ---------- Пошук: Optional ----------

    @Test
    void findByRoomReturnsPresentOptional() {
        Optional<Booking> found = BookingQueries.findByRoom(sample(), 404);
        assertTrue(found.isPresent());
        assertEquals("Tom Cruise", found.get().getGuest());
    }

    @Test
    void findByRoomReturnsEmptyOptionalWhenRoomIsAbsent() {
        Optional<Booking> found = BookingQueries.findByRoom(sample(), 999);
        assertNotNull(found);
        assertTrue(found.isEmpty());
        assertEquals("not found", found.map(Booking::getGuest).orElse("not found"));
    }

    @Test
    void findByRoomWithDuplicatedKeyReturnsFirstInCollectionOrder() {
        assertEquals("John Doe", BookingQueries.findByRoom(sample(), 101).orElseThrow().getGuest());
    }

    // ---------- Порожній вхід ----------

    @Test
    void emptyInputGivesEmptyResultsForEveryQuery() {
        List<Booking> empty = List.of();
        assertTrue(BookingQueries.activeOn(empty, CHECK_IN, CHECK_IN).isEmpty());
        assertTrue(BookingQueries.roomNumbers(empty).isEmpty());
        assertTrue(BookingQueries.countByTariff(empty).isEmpty());
        assertTrue(BookingQueries.longestStays(empty, 5).isEmpty());
        assertTrue(BookingQueries.findByRoom(empty, 101).isEmpty());
        assertTrue(BookingQueries.countByTierAndTariff(empty).isEmpty());
    }

    @Test
    void emptyInputStatisticsHaveZeroCountAndAverage() {
        IntSummaryStatistics stats = BookingQueries.stayStatistics(List.of());
        assertEquals(0, stats.getCount());
        assertEquals(0, stats.getSum());
        assertEquals(0.0, stats.getAverage(), EPS);
        assertEquals(Integer.MAX_VALUE, stats.getMin());
        assertEquals(Integer.MIN_VALUE, stats.getMax());
    }

    @Test
    void emptyInputSummaryIsAllZeros() {
        assertEquals(new NightsRate(0, 0, 0.0, 0), BookingQueries.summarize(List.of()));
        assertEquals(0.0, BookingQueries.totalStayCost(List.of()), EPS);
        assertEquals(0L, BookingQueries.uniqueCount(List.of()));
        assertTrue(BookingQueries.countByTier(List.of()).isEmpty());
    }

    // ---------- Контракт: вхідна колекція не змінюється ----------

    @Test
    void queriesDoNotChangeSourceCollection() {
        List<Booking> source = new ArrayList<>(sample());
        List<Booking> snapshot = new ArrayList<>(source);

        BookingQueries.activeOn(source, CHECK_IN, CHECK_IN.plusDays(2));
        BookingQueries.roomNumbers(source);
        BookingQueries.countByTariff(source);
        BookingQueries.stayStatistics(source);
        BookingQueries.longestStays(source, 5);
        BookingQueries.findByRoom(source, 404);
        BookingQueries.countByTierAndTariff(source);
        BookingQueries.summarize(source);

        assertEquals(snapshot, source);
        for (int i = 0; i < source.size(); i++) {
            assertSame(snapshot.get(i), source.get(i));
        }
    }

    @Test
    void listResultsAreUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> BookingQueries.longestStays(sample(), 5).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> BookingQueries.roomNumbers(sample()).add(1));
        assertThrows(UnsupportedOperationException.class,
                () -> BookingQueries.activeOn(sample(), CHECK_IN, CHECK_IN).clear());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> BookingQueries.roomNumbers(null));
        assertThrows(NullPointerException.class,
                () -> BookingQueries.activeOn(sample(), null, CHECK_IN));
        assertThrows(NullPointerException.class,
                () -> BookingQueries.activeOn(sample(), CHECK_IN, null));
    }

    // ---------- Поліморфна колекція та зірочка ----------

    @Test
    void queriesWorkThroughBaseTypeForBothSubtypes() {
        List<Booking> top = BookingQueries.longestStays(sample(), 3);
        assertInstanceOf(StandardBooking.class, top.get(0));
        assertInstanceOf(StandardBooking.class, top.get(1));
        assertInstanceOf(PremiumBooking.class, top.get(2));
        // 450 + 300 + 7700 + 1463 + 11000 + 1900 + 90
        assertEquals(22903.0, BookingQueries.totalStayCost(sample()), EPS);
        assertEquals(Map.of("Premium", 2L, "Standard", 5L), BookingQueries.countByTier(sample()));
    }

    @Test
    void uniqueCountIgnoresLogicalDuplicates() {
        List<Booking> bookings = List.of(
                Booking.fromCsv("101;John Doe;3;150.00;Standard"),
                Booking.fromCsv("101;John Doe;3;150.00;Standard"),
                Booking.fromCsv("101;Anna Bell;2;150.00;Standard"));
        assertEquals(2L, BookingQueries.uniqueCount(bookings));
    }

    @Test
    void twoLevelGroupingCountsByTierThenTariff() {
        Map<String, Map<RoomKind, Long>> nested = BookingQueries.countByTierAndTariff(sample());
        assertEquals(List.of("Premium", "Standard"), new ArrayList<>(nested.keySet()));
        assertEquals(Map.of(RoomKind.SUITE, 1L, RoomKind.PENTHOUSE, 1L), nested.get("Premium"));
        assertEquals(Map.of(RoomKind.ECONOMY, 1L, RoomKind.STANDARD, 2L, RoomKind.DELUXE, 1L,
                RoomKind.BUSINESS, 1L), nested.get("Standard"));
    }

    @Test
    void summarizeMatchesKnownTotals() {
        NightsRate summary = BookingQueries.summarize(sample());
        assertEquals(7, summary.totalRecords());
        assertEquals(35, summary.totalNights());
        assertEquals(21380.0, summary.totalRevenue(), EPS);
        assertEquals(10, summary.maxNights());
    }
}
