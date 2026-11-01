package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/**
 * Описує незмінний запис про бронювання в готелі.
 */
public final class Booking {
    private final int room;
    private final String guest;
    private final int nights;
    private final double nightlyRate;
    private final String category;

    /**
     * Створює об'єкт бронювання та перевіряє інваріанти.
     */
    public Booking(int room, String guest, int nights, double nightlyRate, String category) {
        if (guest == null || guest.isBlank()) {
            throw new IllegalArgumentException("Guest name is missing");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is missing");
        }
        if (room <= 0) {
            throw new IllegalArgumentException("Invalid room number (must be > 0)");
        }
        if (nights <= 0) {
            throw new IllegalArgumentException("Invalid nights count (must be > 0)");
        }
        if (nightlyRate <= 0.0 || !Double.isFinite(nightlyRate)) {
            throw new IllegalArgumentException("Invalid nightly rate (must be a valid positive number)");
        }

        this.room = room;
        this.guest = guest;
        this.nights = nights;
        this.nightlyRate = nightlyRate;
        this.category = category;
    }

    /**
     * Створює запис із CSV-рядка.
     *
     * @param line рядок у форматі room;guest;nights;nightlyRate;category
     * @return створений запис Booking
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
            return new Booking(room, guestStr, nights, nightlyRate, categoryStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Number parsing error (check ints/doubles)", e);
        }
    }

    public int getRoom() { return room; }
    public String getGuest() { return guest; }
    public int getNights() { return nights; }
    public double getNightlyRate() { return nightlyRate; }
    public String getCategory() { return category; }

    public double getRevenue() { return nights * nightlyRate; }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s), кімната %d: %d ночей, %.2f грн",
                guest, category, room, nights, nightlyRate);
    }
}