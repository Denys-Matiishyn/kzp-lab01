package ua.lpnu.kzp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Другий формат експорту (завдання із зірочкою): JSON-масив об'єктів.
 * Використовує ті самі поля з {@link CsvColumn}, що й {@link CsvExporter}.
 */
public final class JsonExporter {
    /* Забороняє створення екземплярів статичного експортера. */
    private JsonExporter() {
    }

    /**
     * Формує JSON-масив, де кожен запис є об'єктом «назва стовпця: значення».
     * Числа й логічні значення записуються без лапок, решта як рядки.
     *
     * @param <T>     тип записів
     * @param type    клас записів
     * @param records записи для експорту
     * @return текст JSON; для порожнього списку {@code []}
     * @throws DataStorageException якщо клас не має анотованих полів або поле недоступне
     */
    public static <T> String toJson(Class<T> type, List<? extends T> records)
            throws DataStorageException {
        if (records == null) {
            throw new DataStorageException("Список записів для експорту не може бути null");
        }
        List<Field> fields = AnnotatedFields.of(type);
        List<String> objects = new ArrayList<>();
        for (T record : records) {
            List<String> pairs = new ArrayList<>();
            for (Field field : fields) {
                pairs.add(quote(AnnotatedFields.columnName(field)) + ": "
                        + value(AnnotatedFields.read(field, record)));
            }
            objects.add("  {" + String.join(", ", pairs) + "}");
        }
        if (objects.isEmpty()) {
            return "[]";
        }
        return "[\n" + String.join(",\n", objects) + "\n]";
    }

    /* Перетворює значення поля на JSON-значення. */
    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return quote(value.toString());
    }

    /* Бере текст у лапки й екранує службові символи JSON. */
    private static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            switch (symbol) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (symbol < 0x20) {
                        result.append(String.format(Locale.ROOT, "\\u%04x", (int) symbol));
                    } else {
                        result.append(symbol);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}
