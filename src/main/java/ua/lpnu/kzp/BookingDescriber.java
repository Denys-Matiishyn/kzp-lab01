package ua.lpnu.kzp;

/**
 * Завдання із зірочкою: вичерпний {@code switch} зі зіставленням зі зразком
 * над запечатаною ієрархією {@link Booking}. Гілки {@code default} немає:
 * компілятор сам перевіряє, що розглянуто всі дозволені підтипи.
 * Обчислення вартості тут не дублюється, воно залишається в {@link Booking#stayCost()}.
 */
public final class BookingDescriber {
    /* Забороняє створення екземплярів службового класу. */
    private BookingDescriber() {
    }

    /**
     * Описує, яке правило знижки або збору застосовується до бронювання.
     *
     * @param booking бронювання
     * @return текстовий опис правила
     */
    public static String describeRule(Booking booking) {
        return switch (booking) {
            case StandardBooking s -> String.format(java.util.Locale.ROOT,
                    "знижка %.0f%% від %d ночей", s.getLongStayDiscount() * 100,
                    StandardBooking.LONG_STAY_NIGHTS);
            case PremiumBooking p -> String.format(java.util.Locale.ROOT,
                    "сервісний збір %.0f%%", p.getServiceCharge() * 100);
        };
    }
}
