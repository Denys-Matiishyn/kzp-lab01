package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingHierarchyTest {
    private static final double EPS = 0.0001;

    // ---------- Базовий тип: спільні інваріанти діють для обох підтипів ----------

    @Test
    void baseRejectsInvalidCommonStateInBothSubtypes() {
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, null, 1, 10.0, "Standard"));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "  ", 1, 10.0, "Suite"));
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 1, 10.0, " "));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", 1, 10.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(0, "A", 1, 10.0, "Standard"));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(-5, "A", 1, 10.0, "Suite"));
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 0, 10.0, "Standard"));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", -1, 10.0, "Suite"));
    }

    @Test
    void baseRejectsNonPositiveAndNonFiniteRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 1, 0.0, "Standard"));
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 1, -1.0, "Standard"));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", 1, Double.NaN, "Suite"));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", 1, Double.POSITIVE_INFINITY, "Suite"));
    }

    @Test
    void validationMessagesAreKeptFromPreviousLab() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Booking.create(1, "", 1, 10.0, ""));
        assertEquals("Guest name is missing", ex.getMessage());
        ex = assertThrows(IllegalArgumentException.class,
                () -> Booking.create(0, "A", 1, 10.0, "Standard"));
        assertEquals("Invalid room number (must be > 0)", ex.getMessage());
    }

    @Test
    void acceptedCommonFieldsAreAvailableThroughBaseType() {
        Booking booking = new StandardBooking(7, "Ann", 2, 99.5, "Deluxe");
        assertEquals(7, booking.getRoom());
        assertEquals("Ann", booking.getGuest());
        assertEquals(2, booking.getNights());
        assertEquals(99.5, booking.getNightlyRate(), EPS);
        assertEquals("Deluxe", booking.getCategory());
        assertSame(RoomKind.DELUXE, booking.getKind());
        assertEquals(199.0, booking.getRevenue(), EPS);
    }

    // ---------- Фабрика вибирає підтип ----------

    @Test
    void factoryChoosesSubtypeByCategory() {
        assertInstanceOf(StandardBooking.class, Booking.create(1, "A", 1, 10.0, "Economy"));
        assertInstanceOf(StandardBooking.class, Booking.create(1, "A", 1, 10.0, "Unknown"));
        assertInstanceOf(PremiumBooking.class, Booking.create(1, "A", 1, 10.0, "Suite"));
        assertInstanceOf(PremiumBooking.class, Booking.fromCsv("1;A;1;10.0;presidential"));
    }

    // ---------- Підтипи: власні поля й різна поведінка ----------

    @Test
    void standardGivesDiscountOnlyForLongStay() {
        Booking shortStay = new StandardBooking(1, "A", 6, 100.0, "Standard");
        Booking longStay = new StandardBooking(1, "A", 7, 100.0, "Standard");
        assertEquals(600.0, shortStay.stayCost(), EPS);
        assertEquals(665.0, longStay.stayCost(), EPS);
    }

    @Test
    void premiumAddsServiceCharge() {
        Booking premium = new PremiumBooking(1, "A", 2, 500.0, "Suite");
        assertEquals(1100.0, premium.stayCost(), EPS);
    }

    @Test
    void subtypesCalculateCostDifferentlyForSameInput() {
        Booking standard = new StandardBooking(1, "A", 10, 100.0, "Suite");
        Booking premium = new PremiumBooking(1, "A", 10, 100.0, "Suite");
        assertEquals(standard.getRevenue(), premium.getRevenue(), EPS);
        assertNotEquals(standard.stayCost(), premium.stayCost());
    }

    @Test
    void tierLabelsDiffer() {
        assertEquals("Standard", new StandardBooking(1, "A", 1, 1.0, "Standard").tierLabel());
        assertEquals("Premium", new PremiumBooking(1, "A", 1, 1.0, "Suite").tierLabel());
    }

    @Test
    void customDiscountIsApplied() {
        StandardBooking booking = new StandardBooking(1, "A", 10, 100.0, "Standard", 0.5);
        assertEquals(0.5, booking.getLongStayDiscount(), EPS);
        assertEquals(500.0, booking.stayCost(), EPS);
    }

    @Test
    void standardRejectsInvalidOwnDiscount() {
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 1, 10.0, "Standard", -0.01));
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 1, 10.0, "Standard", 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new StandardBooking(1, "A", 1, 10.0, "Standard", Double.NaN));
    }

    @Test
    void premiumAcceptsBoundaryServiceChargeAndRejectsInvalid() {
        assertEquals(100.0, new PremiumBooking(1, "A", 1, 100.0, "Suite", 0.0).stayCost(), EPS);
        assertEquals(200.0, new PremiumBooking(1, "A", 1, 100.0, "Suite", 1.0).stayCost(), EPS);
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", 1, 10.0, "Suite", -0.1));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", 1, 10.0, "Suite", 1.01));
        assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(1, "A", 1, 10.0, "Suite", Double.POSITIVE_INFINITY));
    }

    @Test
    void baseValidationRunsBeforeSubtypeValidation() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PremiumBooking(0, "A", 1, 10.0, "Suite", 5.0));
        assertEquals("Invalid room number (must be > 0)", ex.getMessage());
    }

    // ---------- Поліморфна колекція ----------

    @Test
    void polymorphicCollectionIsProcessedThroughBaseType() {
        List<Booking> bookings = new ArrayList<>();
        bookings.add(new StandardBooking(1, "A", 7, 100.0, "Standard")); // 665
        bookings.add(new PremiumBooking(2, "B", 2, 500.0, "Suite"));      // 1100
        double total = 0.0;
        for (Booking b : bookings) {
            total += b.stayCost();
        }
        assertEquals(1765.0, total, EPS);
    }

    // ---------- equals / hashCode ----------

    @Test
    void equalObjectsHaveSameHashCode() {
        Booking first = new StandardBooking(1, "A", 3, 50.0, "Standard");
        Booking second = new StandardBooking(1, "A", 3, 50.0, "Standard");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalsIsReflexiveSymmetricAndHandlesNullAndOtherTypes() {
        Booking first = new PremiumBooking(1, "A", 3, 50.0, "Suite");
        Booking second = new PremiumBooking(1, "A", 3, 50.0, "Suite");
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertNotEquals(null, first);
        assertNotEquals("text", first);
    }

    @Test
    void differentFieldsMakeObjectsNotEqual() {
        Booking base = new StandardBooking(1, "A", 3, 50.0, "Standard");
        assertNotEquals(base, new StandardBooking(2, "A", 3, 50.0, "Standard"));
        assertNotEquals(base, new StandardBooking(1, "B", 3, 50.0, "Standard"));
        assertNotEquals(base, new StandardBooking(1, "A", 4, 50.0, "Standard"));
        assertNotEquals(base, new StandardBooking(1, "A", 3, 51.0, "Standard"));
        assertNotEquals(base, new StandardBooking(1, "A", 3, 50.0, "Deluxe"));
        assertNotEquals(base, new StandardBooking(1, "A", 3, 50.0, "Standard", 0.2));
    }

    @Test
    void differentSubtypesWithSameDataAreNotEqual() {
        Booking standard = new StandardBooking(1, "A", 3, 50.0, "Suite");
        Booking premium = new PremiumBooking(1, "A", 3, 50.0, "Suite");
        assertNotEquals(standard, premium);
        assertNotEquals(premium, standard);
    }

    @Test
    void hashSetDoesNotDuplicateEqualBookings() {
        Set<Booking> set = new HashSet<>();
        set.add(Booking.fromCsv("101;John Doe;3;150.00;Standard"));
        set.add(Booking.fromCsv("101;John Doe;3;150.00;Standard"));
        set.add(Booking.fromCsv("301;Dexter Morgan;3;1000.00;Suite"));
        set.add(Booking.fromCsv("301;Dexter Morgan;3;1000.00;Suite"));
        assertEquals(2, set.size());
    }

    @Test
    void hashMapFindsValueByLogicallyEqualKey() {
        Map<Booking, String> map = new HashMap<>();
        map.put(new PremiumBooking(5, "Z", 2, 700.0, "Penthouse"), "VIP");
        assertEquals("VIP", map.get(new PremiumBooking(5, "Z", 2, 700.0, "Penthouse")));
        assertEquals(null, map.get(new StandardBooking(5, "Z", 2, 700.0, "Penthouse")));
    }

    // ---------- toString / завдання із зірочкою ----------

    @Test
    void toStringKeepsFormatFromPreviousLab() {
        Booking booking = new StandardBooking(101, "John Doe", 3, 150.0, "Standard");
        assertEquals("John Doe (Standard), кімната 101: 3 ночей, 150.00 грн", booking.toString());
    }

    @Test
    void describerCoversBothPermittedSubtypes() {
        assertEquals("знижка 5% від 7 ночей",
                BookingDescriber.describeRule(new StandardBooking(1, "A", 1, 1.0, "Standard")));
        assertEquals("сервісний збір 10%",
                BookingDescriber.describeRule(new PremiumBooking(1, "A", 1, 1.0, "Suite")));
    }
}
