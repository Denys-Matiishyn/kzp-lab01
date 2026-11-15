package ua.lpnu.kzp;

import java.util.Locale;

/**
 * Категорія номера готелю: фіксована множина значень замість довільного тексту.
 */
public enum RoomKind {
    /** Економ-номер. */
    ECONOMY("Economy", "економ", false),
    /** Стандартний номер. */
    STANDARD("Standard", "стандарт", false),
    /** Номер підвищеної комфортності. */
    DELUXE("Deluxe", "делюкс", false),
    /** Бізнес-номер. */
    BUSINESS("Business", "бізнес", false),
    /** Номер-люкс. */
    SUITE("Suite", "люкс", true),
    /** Президентський номер. */
    PRESIDENTIAL("Presidential", "президентський", true),
    /** Пентхаус. */
    PENTHOUSE("Penthouse", "пентхаус", true),
    /** Категорія, якої немає в переліку (зберігає сумісність із попередньою роботою). */
    OTHER("Other", "інша", false);

    private final String csvName;
    private final String label;
    private final boolean premium;

    /* Конструктор enum використовується JVM для створення фіксованих констант. */
    RoomKind(String csvName, String label, boolean premium) {
        this.csvName = csvName;
        this.label = label;
        this.premium = premium;
    }

    /**
     * Повертає український підпис категорії для звіту.
     *
     * @return текстове подання категорії
     */
    public String label() {
        return label;
    }

    /**
     * Повідомляє, чи належить категорія до преміальних номерів.
     *
     * @return {@code true} для преміальної категорії
     */
    public boolean isPremium() {
        return premium;
    }

    /**
     * Визначає категорію за текстом із CSV без урахування регістру.
     * Невідомий або порожній текст дає {@link #OTHER}, щоб будь-яка непорожня
     * категорія, яку приймала попередня робота, залишалася коректною.
     *
     * @param category текст категорії (може бути {@code null})
     * @return знайдена категорія або {@link #OTHER}
     */
    public static RoomKind fromCategory(String category) {
        if (category == null) {
            return OTHER;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        for (RoomKind kind : values()) {
            if (kind != OTHER && kind.csvName.toLowerCase(Locale.ROOT).equals(normalized)) {
                return kind;
            }
        }
        return OTHER;
    }
}
