package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Плоский запис бронювання для зберігання у CSV (варіант 14).
 * Незмінний; ключ запису: номер кімнати та дата заїзду.
 */
public final class BookingRecord {
    /** Кількість полів запису у CSV. */
    public static final int FIELD_COUNT = 5;

    @CsvColumn("номер")
    private final int room;
    @CsvColumn("гість")
    private final String guest;
    @CsvColumn("заїзд")
    private final LocalDate checkIn;
    @CsvColumn("виїзд")
    private final LocalDate checkOut;
    @CsvColumn("тариф")
    private final double rate;

    /**
     * Створює перевірений запис.
     *
     * @param room     номер кімнати (більше нуля)
     * @param guest    ім'я гостя (не порожнє)
     * @param checkIn  дата заїзду
     * @param checkOut дата виїзду (пізніше за заїзд)
     * @param rate     тариф за ніч (скінченне додатне число)
     */
    public BookingRecord(int room, String guest, LocalDate checkIn, LocalDate checkOut, double rate) {
        this(validRoom(room), validGuest(guest), validDate(checkIn, "Check-in date is missing"),
                validCheckOut(checkIn, checkOut), validRate(rate), true);
    }

    /* Присвоює вже перевірений стан під час внутрішнього делегування. */
    private BookingRecord(int room, String guest, LocalDate checkIn, LocalDate checkOut,
                          double rate, boolean validated) {
        this.room = room;
        this.guest = guest;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.rate = rate;
    }

    /* Перевіряє номер кімнати. */
    private static int validRoom(int room) {
        if (room <= 0) {
            throw new IllegalArgumentException("Invalid room number (must be > 0)");
        }
        return room;
    }

    /* Перевіряє ім'я гостя. */
    private static String validGuest(String guest) {
        if (guest == null || guest.isBlank()) {
            throw new IllegalArgumentException("Guest name is missing");
        }
        return guest;
    }

    /* Перевіряє наявність дати. */
    private static LocalDate validDate(LocalDate date, String message) {
        if (date == null) {
            throw new IllegalArgumentException(message);
        }
        return date;
    }

    /* Перевіряє, що виїзд пізніше за заїзд. */
    private static LocalDate validCheckOut(LocalDate checkIn, LocalDate checkOut) {
        validDate(checkOut, "Check-out date is missing");
        if (checkIn != null && !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        return checkOut;
    }

    /* Перевіряє тариф: додатне скінченне число. */
    private static double validRate(double rate) {
        if (rate <= 0.0 || !Double.isFinite(rate)) {
            throw new IllegalArgumentException("Invalid rate (must be a valid positive number)");
        }
        return rate;
    }

    /**
     * Створює запис із бронювання моделі попередніх робіт. Вхідний формат програми не
     * містить дат, тому заїзд задається параметром, а виїзд дорівнює заїзду плюс ночі.
     *
     * @param booking бронювання будь-якого підтипу
     * @param checkIn дата заїзду
     * @return запис для зберігання
     */
    public static BookingRecord fromBooking(Booking booking, LocalDate checkIn) {
        Objects.requireNonNull(booking, "booking");
        Objects.requireNonNull(checkIn, "checkIn");
        return new BookingRecord(booking.getRoom(), booking.getGuest(), checkIn,
                checkIn.plusDays(booking.getNights()), booking.getNightlyRate());
    }

    /**
     * Відновлює запис із розібраних CSV-полів у порядку стовпців файла:
     * заїзд, виїзд, гість, тариф, номер (порядок Java-імен полів).
     *
     * @param fields поля одного CSV-запису
     * @return відновлений запис
     * @throws DataStorageException якщо полів не п'ять або значення некоректне;
     *                              першопричина доступна через {@code getCause()}
     */
    public static BookingRecord fromFields(List<String> fields) throws DataStorageException {
        if (fields == null || fields.size() != FIELD_COUNT) {
            throw new DataStorageException("Очікується %d полів, отримано %d"
                    .formatted(FIELD_COUNT, fields == null ? 0 : fields.size()));
        }
        LocalDate checkIn = parseDate(fields.get(0), "заїзд");
        LocalDate checkOut = parseDate(fields.get(1), "виїзд");
        String guest = fields.get(2);
        double rate;
        int room;
        try {
            rate = Double.parseDouble(fields.get(3).trim());
        } catch (NumberFormatException exception) {
            throw new DataStorageException(
                    "Некоректне число в полі «тариф»: %s".formatted(fields.get(3)), exception);
        }
        try {
            room = Integer.parseInt(fields.get(4).trim());
        } catch (NumberFormatException exception) {
            throw new DataStorageException(
                    "Некоректне число в полі «номер»: %s".formatted(fields.get(4)), exception);
        }
        try {
            return new BookingRecord(room, guest, checkIn, checkOut, rate);
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException(
                    "Некоректні дані бронювання: %s".formatted(exception.getMessage()), exception);
        }
    }

    /* Розбирає дату у форматі ISO (РРРР-ММ-ДД). */
    private static LocalDate parseDate(String text, String column) throws DataStorageException {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException exception) {
            throw new DataStorageException(
                    "Некоректна дата в полі «%s»: %s".formatted(column, text), exception);
        }
    }

    /**
     * Повертає номер кімнати.
     *
     * @return номер кімнати
     */
    public int getRoom() {
        return room;
    }

    /**
     * Повертає ім'я гостя.
     *
     * @return ім'я гостя
     */
    public String getGuest() {
        return guest;
    }

    /**
     * Повертає дату заїзду.
     *
     * @return дата заїзду
     */
    public LocalDate getCheckIn() {
        return checkIn;
    }

    /**
     * Повертає дату виїзду.
     *
     * @return дата виїзду
     */
    public LocalDate getCheckOut() {
        return checkOut;
    }

    /**
     * Повертає тариф за ніч.
     *
     * @return тариф за ніч
     */
    public double getRate() {
        return rate;
    }

    /**
     * Перевіряє ключ запису: номер кімнати та дату заїзду.
     *
     * @param keyRoom    номер кімнати
     * @param keyCheckIn дата заїзду
     * @return {@code true}, якщо запис має такий ключ
     */
    public boolean hasKey(int keyRoom, LocalDate keyCheckIn) {
        return room == keyRoom && checkIn.equals(keyCheckIn);
    }

    /**
     * Порівнює записи за всіма полями.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо записи логічно рівні
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BookingRecord that)) {
            return false;
        }
        return room == that.room
                && Double.compare(rate, that.rate) == 0
                && guest.equals(that.guest)
                && checkIn.equals(that.checkIn)
                && checkOut.equals(that.checkOut);
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(room, guest, checkIn, checkOut, rate);
    }

    /**
     * Повертає текстове подання запису.
     *
     * @return рядок для діагностики
     */
    @Override
    public String toString() {
        return "BookingRecord[room=%d, guest=%s, checkIn=%s, checkOut=%s, rate=%s]"
                .formatted(room, guest, checkIn, checkOut, rate);
    }
}
