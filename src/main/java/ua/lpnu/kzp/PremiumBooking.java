package ua.lpnu.kzp;

import java.util.Objects;

/**
 * Преміальне бронювання: до виторгу додається сервісний збір.
 */
public final class PremiumBooking extends Booking {
    /** Сервісний збір за замовчуванням (частка від 0 до 1). */
    public static final double DEFAULT_SERVICE_CHARGE = 0.10;

    private final double serviceCharge;

    /**
     * Створює преміальне бронювання з сервісним збором за замовчуванням.
     *
     * @param room        номер кімнати
     * @param guest       ім'я гостя
     * @param nights      кількість ночей
     * @param nightlyRate тариф за ніч
     * @param category    категорія номера
     */
    public PremiumBooking(int room, String guest, int nights,
                          double nightlyRate, String category) {
        this(room, guest, nights, nightlyRate, category, DEFAULT_SERVICE_CHARGE);
    }

    /**
     * Створює преміальне бронювання з власним сервісним збором.
     *
     * @param room          номер кімнати
     * @param guest         ім'я гостя
     * @param nights        кількість ночей
     * @param nightlyRate   тариф за ніч
     * @param category      категорія номера
     * @param serviceCharge сервісний збір як частка в проміжку [0; 1]
     */
    public PremiumBooking(int room, String guest, int nights, double nightlyRate,
                          String category, double serviceCharge) {
        super(room, guest, nights, nightlyRate, category);
        if (!Double.isFinite(serviceCharge) || serviceCharge < 0.0 || serviceCharge > 1.0) {
            throw new IllegalArgumentException("Invalid service charge (must be in [0; 1])");
        }
        this.serviceCharge = serviceCharge;
    }

    /**
     * Повертає сервісний збір.
     *
     * @return частка збору
     */
    public double getServiceCharge() {
        return serviceCharge;
    }

    /**
     * Обчислює вартість: виторг, збільшений на сервісний збір.
     *
     * @return вартість проживання
     */
    @Override
    public double stayCost() {
        return getRevenue() * (1.0 + serviceCharge);
    }

    /**
     * Повертає назву рівня обслуговування.
     *
     * @return {@code "Premium"}
     */
    @Override
    public String tierLabel() {
        return "Premium";
    }

    /**
     * Порівнює бронювання з урахуванням сервісного збору.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо об'єкти логічно рівні
     */
    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && Double.compare(serviceCharge, ((PremiumBooking) other).serviceCharge) == 0;
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), serviceCharge);
    }
}
