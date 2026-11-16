package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * @param <T> тип записів, які приймає сховище
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /** Створює порожнє сховище. */
    public Repository() {
    }

    /**
     * Додає запис до сховища.
     *
     * @param item запис (не {@code null})
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "item"));
    }

    /**
     * Повертає незмінний знімок усіх записів у порядку додавання.
     * Подальші зміни сховища на вже отриманий знімок не впливають.
     *
     * @return незмінна копія вмісту сховища
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову.
     *
     * @param condition умова для {@code T} або його супертипу
     * @return незмінний список знайдених записів у порядку додавання
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "condition");
        return items.stream().filter(condition).toList();
    }

    /**
     * Повертає кількість записів.
     *
     * @return кількість записів у сховищі
     */
    public int size() {
        return items.size();
    }
}
