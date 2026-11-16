package ua.lpnu.kzp;

/**
 * Повідомляє про помилку зберігання, експорту або читання даних.
 * Першопричина (наприклад, {@link java.io.IOException}) зберігається в {@link #getCause()}.
 */
public class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток із повідомленням.
     *
     * @param message пояснення, яка операція не вдалася
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток із повідомленням і першопричиною.
     *
     * @param message пояснення, яка операція не вдалася
     * @param cause   початкова технічна помилка
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
