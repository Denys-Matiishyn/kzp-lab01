package ua.lpnu.kzp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Зберігає назву стовпця експорту для поля.
 * Анотація доступна під час виконання, тому експортер читає її рефлексією.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /**
     * Повертає заголовок стовпця.
     *
     * @return назва стовпця у файлі
     */
    String value();
}
