package ua.lpnu.kzp;

import java.util.Objects;

/**
 * Бронювання стандартного рівня: вартість дорівнює виторгу, а для тривалого
 * проживання (від {@value #LONG_STAY_NIGHTS} ночей) діє знижка.
 */
public final class StandardBooking extends Booking {
    /** Мінімальна кількість ночей, з якої діє знижка. */
    public static final int LONG_STAY_NIGHTS = 7;
    /** Знижка за замовчуванням (частка від 0 до 1). */
    public static final double DEFAULT_LONG_STAY_DISCOUNT = 0.05;

    private final double longStayDiscount;

    /**
     * Створює стандартне бронювання зі знижкою за замовчуванням.
     *
     * @param room        номер кімнати
     * @param guest       ім'я гостя
     * @param nights      кількість ночей
     * @param nightlyRate тариф за ніч
     * @param category    категорія номера
     */
    public StandardBooking(int room, String guest, int nights,
                           double nightlyRate, String category) {
        this(room, guest, nights, nightlyRate, category, DEFAULT_LONG_STAY_DISCOUNT);
    }

    /**
     * Створює стандартне бронювання з власною знижкою за тривале проживання.
     *
     * @param room             номер кімнати
     * @param guest            ім'я гостя
     * @param nights           кількість ночей
     * @param nightlyRate      тариф за ніч
     * @param category         категорія номера
     * @param longStayDiscount знижка як частка в проміжку [0; 1)
     */
    public StandardBooking(int room, String guest, int nights, double nightlyRate,
                           String category, double longStayDiscount) {
        super(room, guest, nights, nightlyRate, category);
        if (!Double.isFinite(longStayDiscount) || longStayDiscount < 0.0
                || longStayDiscount >= 1.0) {
            throw new IllegalArgumentException("Invalid long-stay discount (must be in [0; 1))");
        }
        this.longStayDiscount = longStayDiscount;
    }

    /**
     * Повертає знижку за тривале проживання.
     *
     * @return частка знижки
     */
    public double getLongStayDiscount() {
        return longStayDiscount;
    }

    /**
     * Обчислює вартість: виторг, зменшений на знижку для тривалого проживання.
     *
     * @return вартість проживання
     */
    @Override
    public double stayCost() {
        if (getNights() >= LONG_STAY_NIGHTS) {
            return getRevenue() * (1.0 - longStayDiscount);
        }
        return getRevenue();
    }

    /**
     * Повертає назву рівня обслуговування.
     *
     * @return {@code "Standard"}
     */
    @Override
    public String tierLabel() {
        return "Standard";
    }

    /**
     * Порівнює бронювання з урахуванням знижки.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо об'єкти логічно рівні
     */
    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && Double.compare(longStayDiscount, ((StandardBooking) other).longStayDiscount) == 0;
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), longStayDiscount);
    }
}
