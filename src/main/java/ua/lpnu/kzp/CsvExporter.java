package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Універсальний CSV-експортер. Працює з будь-яким класом, що має поля з
 * {@link CsvColumn}: назви класів і полів предметної області тут не згадуються.
 */
public final class CsvExporter {
    /* Забороняє створення екземплярів статичного експортера. */
    private CsvExporter() {
    }

    /**
     * Записує анотовані поля записів у CSV-файл у кодуванні UTF-8.
     * Наявний файл перезаписується повністю; відсутні батьківські каталоги створюються.
     *
     * @param <T>     тип записів
     * @param path    шлях до CSV-файла
     * @param type    клас записів
     * @param records записи для експорту
     * @throws DataStorageException якщо поля неможливо прочитати або файл записати
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        if (path == null) {
            throw new DataStorageException("Не задано шлях до CSV-файла");
        }
        String csv = toCsv(type, records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося записати CSV: %s".formatted(path), exception);
        }
    }

    /**
     * Формує текст CSV: рядок заголовка та по одному рядку на запис.
     * Записи розділяються системним роздільником рядків.
     *
     * @param <T>     тип записів
     * @param type    клас записів
     * @param records записи для експорту (список не змінюється)
     * @return текст CSV-документа
     * @throws DataStorageException якщо клас не має анотованих полів або поле недоступне
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records)
            throws DataStorageException {
        if (records == null) {
            throw new DataStorageException("Список записів для експорту не може бути null");
        }
        // Один набір полів використовується і для заголовка, і для всіх рядків.
        List<Field> fields = AnnotatedFields.of(type);
        StringBuilder csv = new StringBuilder(header(fields)).append(System.lineSeparator());
        for (T record : records) {
            List<String> values = new ArrayList<>();
            for (Field field : fields) {
                values.add(escape(AnnotatedFields.read(field, record)));
            }
            csv.append(String.join(",", values)).append(System.lineSeparator());
        }
        return csv.toString();
    }

    /**
     * Повертає рядок заголовка для класу: назви з {@link CsvColumn} у порядку імен полів.
     *
     * @param type клас записів
     * @return заголовок без роздільника рядків
     * @throws DataStorageException якщо клас не має анотованих полів
     */
    public static String header(Class<?> type) throws DataStorageException {
        return header(AnnotatedFields.of(type));
    }

    /* Збирає заголовок з уже знайдених полів. */
    private static String header(List<Field> fields) {
        List<String> names = new ArrayList<>();
        for (Field field : fields) {
            names.add(escape(AnnotatedFields.columnName(field)));
        }
        return String.join(",", names);
    }

    /**
     * Екранує значення за правилами CSV: поле з комою, лапкою або перенесенням рядка
     * береться в подвійні лапки, а лапка всередині подвоюється.
     *
     * @param value значення поля; {@code null} дає порожнє поле
     * @return текст поля для запису у CSV
     */
    public static String escape(Object value) {
        String text = Objects.toString(value, "");
        boolean quoted = text.contains(",")
                || text.contains("\"")
                || text.contains("\r")
                || text.contains("\n");
        return quoted ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
