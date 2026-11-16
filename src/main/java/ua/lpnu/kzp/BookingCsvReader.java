package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Читає CSV із записами бронювань і відновлює об'єкти {@link BookingRecord}.
 * Це предметна частина імпорту: універсальний експортер про неї не знає.
 */
public final class BookingCsvReader {
    /* Забороняє створення екземплярів статичного читача. */
    private BookingCsvReader() {
    }

    /**
     * Читає файл у кодуванні UTF-8 і відновлює записи.
     *
     * @param path шлях до CSV-файла
     * @return незмінний список записів у порядку файла
     * @throws DataStorageException якщо файл неможливо прочитати (причина — {@link IOException})
     *                              або його вміст некоректний
     */
    public static List<BookingRecord> read(Path path) throws DataStorageException {
        if (path == null) {
            throw new DataStorageException("Не задано шлях до CSV-файла");
        }
        String text;
        try {
            // Увесь текст читається разом: поле в лапках може містити перенесення рядка.
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(path), exception);
        }
        return parse(text);
    }

    /**
     * Відновлює записи з тексту CSV. Порожній текст і текст лише із заголовком
     * дають порожній список; інший заголовок вважається помилкою формату.
     *
     * @param text текст CSV-документа
     * @return незмінний список записів
     * @throws DataStorageException якщо заголовок або будь-який запис некоректний;
     *                              повідомлення містить номер запису
     */
    public static List<BookingRecord> parse(String text) throws DataStorageException {
        List<List<String>> rows = CsvParser.parseDocument(text);
        if (rows.isEmpty()) {
            return List.of();
        }
        String expectedHeader = CsvExporter.header(BookingRecord.class);
        String actualHeader = String.join(",", rows.get(0));
        if (!expectedHeader.equals(actualHeader)) {
            throw new DataStorageException(
                    "Невідомий заголовок CSV: очікується «%s», отримано «%s»"
                            .formatted(expectedHeader, actualHeader));
        }
        List<BookingRecord> records = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            try {
                records.add(BookingRecord.fromFields(rows.get(index)));
            } catch (DataStorageException exception) {
                // Додається контекст (номер запису), першопричина не губиться.
                throw new DataStorageException(
                        "CSV-запис %d: %s".formatted(index, exception.getMessage()), exception);
            }
        }
        return List.copyOf(records);
    }
}
