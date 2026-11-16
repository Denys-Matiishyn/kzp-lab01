package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExporterTest {

    /** Клас з іншої предметної області: доводить, що експортер не прив'язаний до готелю. */
    private static class City {
        @CsvColumn("Назва")
        private final String name;
        @CsvColumn("Населення")
        private final int population;
        private final String note;

        City(String name, int population) {
            this.name = name;
            this.population = population;
            this.note = "не експортується";
        }

        String note() {
            return note + name + population;
        }
    }

    /** Підклас додає поле; успадковані анотовані поля також експортуються. */
    private static final class Capital extends City {
        @CsvColumn("Країна")
        private final String country;

        Capital(String name, int population, String country) {
            super(name, population);
            this.country = country;
        }

        String country() {
            return country;
        }
    }

    private static final class NoColumns {
        private final int value = 1;

        int value() {
            return value;
        }
    }

    private static String lines(String... lines) {
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    // ---------- Анотація ----------

    @Test
    void annotationIsAvailableAtRuntimeOnFields() throws Exception {
        assertEquals(RetentionPolicy.RUNTIME,
                CsvColumn.class.getAnnotation(java.lang.annotation.Retention.class).value());
        assertEquals(List.of(ElementType.FIELD),
                List.of(CsvColumn.class.getAnnotation(java.lang.annotation.Target.class).value()));

        Field guest = BookingRecord.class.getDeclaredField("guest");
        assertNotNull(guest.getAnnotation(CsvColumn.class));
        assertEquals("гість", guest.getAnnotation(CsvColumn.class).value());
    }

    @Test
    void everyBookingRecordFieldHasColumnName() {
        List<String> names = new ArrayList<>();
        for (Field field : BookingRecord.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                assertTrue(field.isAnnotationPresent(CsvColumn.class), field.getName());
                names.add(field.getAnnotation(CsvColumn.class).value());
            }
        }
        assertEquals(5, names.size());
        assertTrue(names.containsAll(List.of("номер", "гість", "заїзд", "виїзд", "тариф")));
    }

    // ---------- Заголовок і порядок ----------

    @Test
    void headerUsesAnnotationNamesInFieldNameOrder() throws DataStorageException {
        // Java-імена полів: checkIn, checkOut, guest, rate, room
        assertEquals("заїзд,виїзд,гість,тариф,номер", CsvExporter.header(BookingRecord.class));
    }

    @Test
    void exporterWorksWithArbitraryClassAndSkipsUnannotatedFields() throws DataStorageException {
        String csv = CsvExporter.toCsv(City.class, List.of(new City("Львів", 717000), new City("Київ", 2952000)));
        assertEquals(lines("Назва,Населення", "Львів,717000", "Київ,2952000"), csv);
    }

    @Test
    void inheritedAnnotatedFieldsAreExported() throws DataStorageException {
        // імена полів: country, name, population
        String csv = CsvExporter.toCsv(Capital.class, List.of(new Capital("Київ", 2952000, "Україна")));
        assertEquals(lines("Країна,Назва,Населення", "Україна,Київ,2952000"), csv);
    }

    @Test
    void emptyListGivesHeaderOnly() throws DataStorageException {
        assertEquals(lines("заїзд,виїзд,гість,тариф,номер"),
                CsvExporter.toCsv(BookingRecord.class, List.of()));
    }

    // ---------- Екранування ----------

    @Test
    void escapeLeavesPlainTextAndHandlesNull() {
        assertEquals("John Doe", CsvExporter.escape("John Doe"));
        assertEquals("150.0", CsvExporter.escape(150.0));
        assertEquals("", CsvExporter.escape(null));
        assertEquals("", CsvExporter.escape(""));
    }

    @Test
    void escapeQuotesCommaQuoteAndLineBreaks() {
        assertEquals("\"Smith, John\"", CsvExporter.escape("Smith, John"));
        assertEquals("\"He said \"\"hi\"\"\"", CsvExporter.escape("He said \"hi\""));
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"));
        assertEquals("\"line1\r\nline2\"", CsvExporter.escape("line1\r\nline2"));
    }

    // ---------- Помилки рефлексії ----------

    @Test
    void classWithoutAnnotatedFieldsIsRejected() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(NoColumns.class, List.of(new NoColumns())));
        assertTrue(exception.getMessage().contains("NoColumns"));
        assertNull(exception.getCause());
    }

    @Test
    void nullArgumentsAreReportedAsStorageErrors() {
        assertThrows(DataStorageException.class, () -> CsvExporter.toCsv(null, List.of()));
        assertThrows(DataStorageException.class, () -> CsvExporter.toCsv(City.class, null));
        assertThrows(DataStorageException.class,
                () -> CsvExporter.write(null, City.class, List.of()));
        List<City> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(DataStorageException.class, () -> CsvExporter.toCsv(City.class, withNull));
    }

    // ---------- Файл ----------

    @Test
    void writeCreatesUtf8FileAndParentDirectories(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("nested").resolve("cities.csv");
        CsvExporter.write(file, City.class, List.of(new City("Львів", 717000)));

        byte[] bytes = Files.readAllBytes(file);
        assertEquals(lines("Назва,Населення", "Львів,717000"), new String(bytes, StandardCharsets.UTF_8));
        // кирилична літера в UTF-8 займає два байти: «Назва» = 10 байтів
        assertEquals(10, "Назва".getBytes(StandardCharsets.UTF_8).length);
        assertEquals((byte) 0xD0, bytes[0]);
    }

    @Test
    void writeTruncatesPreviousLongerFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cities.csv");
        CsvExporter.write(file, City.class,
                List.of(new City("Львів", 717000), new City("Київ", 2952000), new City("Одеса", 1010000)));
        CsvExporter.write(file, City.class, List.of(new City("Львів", 717000)));
        assertEquals(lines("Назва,Населення", "Львів,717000"), Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void writeToDirectoryPathFailsWithIoCause(@TempDir Path dir) {
        // шлях указує на наявний каталог, а не на файл
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(dir, City.class, List.of(new City("Львів", 717000))));
        assertTrue(exception.getMessage().startsWith("Не вдалося записати CSV"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void exportDoesNotChangeRecordsList() throws DataStorageException {
        List<City> cities = new ArrayList<>(List.of(new City("Львів", 717000), new City("Київ", 2952000)));
        List<City> snapshot = new ArrayList<>(cities);
        CsvExporter.toCsv(City.class, cities);
        assertEquals(snapshot, cities);
        assertEquals("не експортуєтьсяЛьвів717000", cities.get(0).note());
        assertEquals("Україна", new Capital("Київ", 1, "Україна").country());
        assertEquals(1, new NoColumns().value());
    }
}
