package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/**
 * Спільний абстрактний тип запису про бронювання в готелі.
 *
 * <p>Тут зберігаються спільні поля й інваріанти всіх підтипів. Різна поведінка
 * (вартість проживання) винесена в абстрактний метод {@link #stayCost()}.
 * Ієрархія запечатана: дозволені лише {@link StandardBooking} і {@link PremiumBooking}.
 */
public abstract sealed class Booking permits StandardBooking, PremiumBooking {
    private final int room;
    private final String guest;
    private final int nights;
    private final double nightlyRate;
    private final String category;
    private final RoomKind kind;

    /**
     * Створює бронювання з перевіреними спільними полями.
     *
     * @param room        номер кімнати (більше нуля)
     * @param guest       ім'я гостя (не порожнє)
     * @param nights      кількість ночей (більше нуля)
     * @param nightlyRate тариф за ніч (скінченне додатне число)
     * @param category    категорія номера (не порожня)
     */
    protected Booking(int room, String guest, int nights, double nightlyRate, String category) {
        // Порядок аргументів визначає порядок перевірок (як у попередній роботі).
        this(validateGuest(guest), validateCategory(category), validateRoom(room),
                validateNights(nights), validateRate(nightlyRate), true);
    }

    /* Присвоює вже перевірений стан під час внутрішнього делегування. */
    private Booking(String guest, String category, int room, int nights,
                    double nightlyRate, boolean validated) {
        this.guest = guest;
        this.category = category;
        this.room = room;
        this.nights = nights;
        this.nightlyRate = nightlyRate;
        this.kind = RoomKind.fromCategory(category);
    }

    /* Перевіряє ім'я гостя. */
    private static String validateGuest(String guest) {
        if (guest == null || guest.isBlank()) {
            throw new IllegalArgumentException("Guest name is missing");
        }
        return guest;
    }

    /* Перевіряє категорію. */
    private static String validateCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is missing");
        }
        return category;
    }

    /* Перевіряє номер кімнати. */
    private static int validateRoom(int room) {
        if (room <= 0) {
            throw new IllegalArgumentException("Invalid room number (must be > 0)");
        }
        return room;
    }

    /* Перевіряє кількість ночей. */
    private static int validateNights(int nights) {
        if (nights <= 0) {
            throw new IllegalArgumentException("Invalid nights count (must be > 0)");
        }
        return nights;
    }

    /* Перевіряє тариф: додатне скінченне число. */
    private static double validateRate(double nightlyRate) {
        if (nightlyRate <= 0.0 || !Double.isFinite(nightlyRate)) {
            throw new IllegalArgumentException(
                    "Invalid nightly rate (must be a valid positive number)");
        }
        return nightlyRate;
    }

    /**
     * Фабричний метод: обирає підтип за категорією номера.
     * Це єдине місце, де враховується категорія; далі всі об'єкти
     * обробляються через спільний тип без перевірок підтипу.
     *
     * @param room        номер кімнати
     * @param guest       ім'я гостя
     * @param nights      кількість ночей
     * @param nightlyRate тариф за ніч
     * @param category    категорія номера
     * @return {@link PremiumBooking} для преміальних категорій, інакше {@link StandardBooking}
     */
    public static Booking create(int room, String guest, int nights,
                                 double nightlyRate, String category) {
        if (RoomKind.fromCategory(category).isPremium()) {
            return new PremiumBooking(room, guest, nights, nightlyRate, category);
        }
        return new StandardBooking(room, guest, nights, nightlyRate, category);
    }

    /**
     * Створює запис із CSV-рядка.
     *
     * @param line рядок у форматі room;guest;nights;nightlyRate;category
     * @return створений запис потрібного підтипу
     */
    public static Booking fromCsv(String line) {
        Objects.requireNonNull(line, "Рядок не може бути null");
        String[] parts = line.split(";", -1);

        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid format (expected 5 columns)");
        }

        String roomStr = parts[0].trim();
        String guestStr = parts[1].trim();
        String nightsStr = parts[2].trim();
        String rateStr = parts[3].trim();
        String categoryStr = parts[4].trim();

        if (roomStr.isEmpty()) {
            throw new IllegalArgumentException("Room number is missing");
        }
        if (guestStr.isEmpty()) {
            throw new IllegalArgumentException("Guest name is missing");
        }
        if (categoryStr.isEmpty()) {
            throw new IllegalArgumentException("Category is missing");
        }

        try {
            int room = Integer.parseInt(roomStr);
            int nights = Integer.parseInt(nightsStr);
            double nightlyRate = Double.parseDouble(rateStr);
            return create(room, guestStr, nights, nightlyRate, categoryStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Number parsing error (check ints/doubles)", e);
        }
    }

    /**
     * Повертає номер кімнати.
     *
     * @return номер кімнати
     */
    public final int getRoom() {
        return room;
    }

    /**
     * Повертає ім'я гостя.
     *
     * @return ім'я гостя
     */
    public final String getGuest() {
        return guest;
    }

    /**
     * Повертає кількість ночей.
     *
     * @return кількість ночей
     */
    public final int getNights() {
        return nights;
    }

    /**
     * Повертає тариф за ніч.
     *
     * @return тариф за ніч
     */
    public final double getNightlyRate() {
        return nightlyRate;
    }

    /**
     * Повертає категорію так, як її задано у вхідних даних.
     *
     * @return текст категорії
     */
    public final String getCategory() {
        return category;
    }

    /**
     * Повертає категорію номера як значення enum.
     *
     * @return категорія номера
     */
    public final RoomKind getKind() {
        return kind;
    }

    /**
     * Повертає базовий виторг за проживання (ночі × тариф).
     * Саме цей показник входить до звіту, як і в попередній роботі.
     *
     * @return ночі × тариф
     */
    public final double getRevenue() {
        return nights * nightlyRate;
    }

    /**
     * Обчислює вартість проживання за правилами конкретного підтипу.
     *
     * @return вартість проживання в гривнях
     */
    public abstract double stayCost();

    /**
     * Повертає назву рівня обслуговування для звіту.
     *
     * @return назва рівня
     */
    public abstract String tierLabel();

    /**
     * Порівнює бронювання за підтипом і всіма полями запису.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо об'єкти логічно рівні
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // getClass не допускає рівність різних підтипів із однаковими даними.
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Booking that = (Booking) other;
        return room == that.room
                && nights == that.nights
                && Double.compare(nightlyRate, that.nightlyRate) == 0
                && guest.equals(that.guest)
                && category.equals(that.category);
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(getClass(), room, guest, nights, nightlyRate, category);
    }

    /**
     * Повертає текстове подання бронювання.
     *
     * @return рядок для звіту
     */
    @Override
    public final String toString() {
        return String.format(Locale.ROOT, "%s (%s), кімната %d: %d ночей, %.2f грн",
                guest, category, room, nights, nightlyRate);
    }
}
