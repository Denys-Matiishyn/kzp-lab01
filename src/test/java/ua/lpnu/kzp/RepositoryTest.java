package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class RepositoryTest {

    @Test
    void newRepositoryIsEmpty() {
        Repository<String> repository = new Repository<>();
        assertEquals(0, repository.size());
        assertTrue(repository.all().isEmpty());
        assertTrue(repository.find(text -> true).isEmpty());
    }

    @Test
    void addKeepsInsertionOrder() {
        Repository<String> repository = new Repository<>();
        repository.add("b");
        repository.add("a");
        repository.add("b");
        assertEquals(List.of("b", "a", "b"), repository.all());
        assertEquals(3, repository.size());
    }

    @Test
    void addRejectsNull() {
        Repository<String> repository = new Repository<>();
        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertEquals(0, repository.size());
    }

    @Test
    void allReturnsUnmodifiableSnapshot() {
        Repository<String> repository = new Repository<>();
        repository.add("a");
        List<String> snapshot = repository.all();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("x"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);

        repository.add("b");
        // раніше отриманий знімок не змінюється разом зі сховищем
        assertEquals(List.of("a"), snapshot);
        assertEquals(List.of("a", "b"), repository.all());
    }

    @Test
    void findFiltersByPredicate() {
        Repository<Integer> repository = new Repository<>();
        for (int value = 1; value <= 6; value++) {
            repository.add(value);
        }
        assertEquals(List.of(2, 4, 6), repository.find(value -> value % 2 == 0));
        assertTrue(repository.find(value -> value > 100).isEmpty());
        assertThrows(NullPointerException.class, () -> repository.find(null));
    }

    @Test
    void findAcceptsPredicateOfSupertype() {
        Repository<Booking> repository = new Repository<>();
        repository.add(Booking.fromCsv("101;John Doe;3;150.00;Standard"));
        repository.add(Booking.fromCsv("301;Dexter Morgan;7;1000.00;Suite"));

        // умова для Object підходить завдяки Predicate<? super T>
        Predicate<Object> isPremium = item -> item instanceof PremiumBooking;
        List<Booking> premium = repository.find(isPremium);
        assertEquals(1, premium.size());
        assertEquals("Dexter Morgan", premium.get(0).getGuest());
    }

    @Test
    void repositoryStoresPolymorphicObjects() {
        Repository<Booking> repository = new Repository<>();
        repository.add(new StandardBooking(1, "A", 2, 10.0, "Standard"));
        repository.add(new PremiumBooking(2, "B", 2, 10.0, "Suite"));
        assertEquals(List.of("Standard", "Premium"),
                repository.all().stream().map(Booking::tierLabel).toList());
    }
}
