package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoomKindTest {

    @Test
    void knownCategoriesAreRecognizedIgnoringCase() {
        assertSame(RoomKind.SUITE, RoomKind.fromCategory("Suite"));
        assertSame(RoomKind.SUITE, RoomKind.fromCategory("  sUiTe "));
        assertSame(RoomKind.ECONOMY, RoomKind.fromCategory("ECONOMY"));
        assertSame(RoomKind.PENTHOUSE, RoomKind.fromCategory("Penthouse"));
    }

    @Test
    void unknownOrMissingCategoryFallsBackToOther() {
        assertSame(RoomKind.OTHER, RoomKind.fromCategory("Hostel"));
        assertSame(RoomKind.OTHER, RoomKind.fromCategory(""));
        assertSame(RoomKind.OTHER, RoomKind.fromCategory(null));
    }

    @Test
    void premiumFlagMatchesCategories() {
        assertTrue(RoomKind.SUITE.isPremium());
        assertTrue(RoomKind.PRESIDENTIAL.isPremium());
        assertTrue(RoomKind.PENTHOUSE.isPremium());
        assertFalse(RoomKind.ECONOMY.isPremium());
        assertFalse(RoomKind.STANDARD.isPremium());
        assertFalse(RoomKind.DELUXE.isPremium());
        assertFalse(RoomKind.BUSINESS.isPremium());
        assertFalse(RoomKind.OTHER.isPremium());
    }

    @Test
    void labelsAreNotBlankAndEnumValuesAreComparedByIdentity() {
        for (RoomKind kind : RoomKind.values()) {
            assertFalse(kind.label().isBlank());
        }
        assertEquals("люкс", RoomKind.SUITE.label());
        assertSame(RoomKind.valueOf("DELUXE"), RoomKind.DELUXE);
        assertEquals(8, RoomKind.values().length);
    }
}
