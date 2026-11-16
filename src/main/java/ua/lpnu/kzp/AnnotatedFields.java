package ua.lpnu.kzp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Знаходить рефлексією поля з {@link CsvColumn} і читає їхні значення.
 * Спільна частина всіх експортерів: вона не знає жодного предметного класу.
 */
final class AnnotatedFields {
    /* Забороняє створення екземплярів службового класу. */
    private AnnotatedFields() {
    }

    /**
     * Повертає анотовані поля класу та його предків у порядку Java-імен полів.
     * Сортування робить порядок стовпців незалежним від порядку, який повертає JVM.
     *
     * @param type клас записів
     * @return непорожній список доступних полів
     * @throws DataStorageException якщо анотованих полів немає або доступ заборонено
     */
    static List<Field> of(Class<?> type) throws DataStorageException {
        if (type == null) {
            throw new DataStorageException("Не задано клас записів для експорту");
        }
        List<Field> fields = new ArrayList<>();
        // Ланцюжок успадкування проходиться до Object.
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                    .forEach(fields::add);
        }
        if (fields.isEmpty()) {
            throw new DataStorageException(
                    "Клас %s не має полів із @CsvColumn".formatted(type.getSimpleName()));
        }
        fields.sort(Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!field.trySetAccessible()) {
                throw new DataStorageException(
                        "Немає доступу до поля %s".formatted(field.getName()));
            }
        }
        return List.copyOf(fields);
    }

    /**
     * Повертає назву стовпця з анотації поля.
     *
     * @param field анотоване поле
     * @return значення {@link CsvColumn#value()}
     */
    static String columnName(Field field) {
        return field.getAnnotation(CsvColumn.class).value();
    }

    /**
     * Читає значення поля в об'єкті.
     *
     * @param field  поле
     * @param record об'єкт
     * @return значення поля (може бути {@code null})
     * @throws DataStorageException якщо значення неможливо прочитати
     */
    static Object read(Field field, Object record) throws DataStorageException {
        if (record == null) {
            throw new DataStorageException("Запис для експорту не може бути null");
        }
        try {
            return field.get(record);
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            throw new DataStorageException(
                    "Не вдалося прочитати поле %s".formatted(field.getName()), exception);
        }
    }
}
