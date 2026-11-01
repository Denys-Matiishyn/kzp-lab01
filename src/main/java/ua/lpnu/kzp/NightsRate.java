package ua.lpnu.kzp;

/**
 * Зберігає незмінні підсумкові показники бронювань.
 */
public record NightsRate(int totalRecords, int totalNights, double totalRevenue, int maxNights) {
    public NightsRate {
        if (totalRecords < 0 || totalNights < 0 || totalRevenue < 0 || maxNights < 0
                || !Double.isFinite(totalRevenue)) {
            throw new IllegalArgumentException("Показники не можуть бути від'ємними");
        }
    }
}