package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/**
 * Розбирає CSV-документ машиною станів. На відміну від {@code split(",")}, розрізняє
 * кому й перенесення рядка всередині лапок (дані) та поза лапками (роздільники).
 */
public final class CsvParser {
    /* Забороняє створення екземплярів статичного парсера. */
    private CsvParser() {
    }

    /**
     * Розбирає весь CSV-документ на записи. Запис може займати кілька фізичних рядків,
     * якщо поле в лапках містить перенесення рядка. Роздільниками записів є
     * {@code \n}, {@code \r\n} і {@code \r}.
     *
     * @param text текст документа
     * @return незмінний список записів, кожен запис є списком полів;
     *         для порожнього тексту список порожній
     * @throws DataStorageException якщо лапки не закрито або після закритих лапок є текст
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        if (text == null) {
            throw new DataStorageException("CSV-документ не може бути null");
        }
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        boolean recordStarted = false;

        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            if (quoted) {
                // Усередині лапок кома й перенесення рядка є даними.
                if (symbol == '"' && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else if (symbol == '"') {
                    quoted = false;
                    quoteClosed = true;
                } else {
                    current.append(symbol);
                }
            } else if (symbol == '"' && !quoteClosed && current.length() == 0) {
                quoted = true;
                recordStarted = true;
            } else if (symbol == ',') {
                fields.add(current.toString());
                current.setLength(0);
                quoteClosed = false;
                recordStarted = true;
            } else if (symbol == '\r' || symbol == '\n') {
                fields.add(current.toString());
                records.add(List.copyOf(fields));
                fields = new ArrayList<>();
                current.setLength(0);
                quoteClosed = false;
                recordStarted = false;
                if (symbol == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                if (quoteClosed) {
                    throw new DataStorageException(
                            "Символ після закритих CSV-лапок у записі %d".formatted(records.size() + 1));
                }
                if (symbol == '"') {
                    throw new DataStorageException(
                            "Лапка всередині поля без лапок у записі %d".formatted(records.size() + 1));
                }
                current.append(symbol);
                recordStarted = true;
            }
        }
        if (quoted) {
            throw new DataStorageException(
                    "Незакриті CSV-лапки в записі %d".formatted(records.size() + 1));
        }
        // Останній запис без завершального перенесення рядка.
        if (recordStarted) {
            fields.add(current.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }

    /**
     * Розбирає один CSV-запис.
     *
     * @param line текст одного запису
     * @return поля запису
     * @throws DataStorageException якщо текст містить не рівно один запис або має хибний формат
     */
    public static List<String> parseLine(String line) throws DataStorageException {
        List<List<String>> records = parseDocument(line);
        if (records.size() != 1) {
            throw new DataStorageException(
                    "Очікується один CSV-запис, отримано %d".formatted(records.size()));
        }
        return records.get(0);
    }
}
