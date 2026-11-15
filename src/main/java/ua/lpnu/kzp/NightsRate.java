package ua.lpnu.kzp;

/**
 * Зберігає незмінні підсумкові показники бронювань.
 *
 * @param totalRecords кількість коректних записів
 * @param totalNights  сумарна кількість ночей
 * @param totalRevenue загальний виторг
 * @param maxNights    найдовше проживання
 */
public record NightsRate(int totalRecords, int totalNights, double totalRevenue, int maxNights) {
    /** Перевіряє, що показники невід'ємні й виторг скінченний. */
    public NightsRate {
        if (totalRecords < 0 || totalNights < 0 || totalRevenue < 0 || maxNights < 0
                || !Double.isFinite(totalRevenue)) {
            throw new IllegalArgumentException("Показники не можуть бути від'ємними");
        }
    }
}