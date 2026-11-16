package ua.lpnu.kzp;

import java.util.List;

/**
 * Спільний контракт форматів експорту (CSV, JSON): перетворює записи з полями
 * {@link CsvColumn} на текст.
 *
 * @param <T> тип записів
 */
@FunctionalInterface
public interface Exporter<T> {
    /**
     * Формує текст документа.
     *
     * @param type    клас записів (потрібен для заголовка навіть без записів)
     * @param records записи для експорту
     * @return текст документа
     * @throws DataStorageException якщо поля неможливо знайти або прочитати
     */
    String export(Class<T> type, List<? extends T> records) throws DataStorageException;
}
