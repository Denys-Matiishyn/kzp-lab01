# Lab 5: Cross-platform Programming (Hotel Data Processing)

## Призначення
Консольний Java-застосунок для лабораторної роботи № 5 (варіант 14, «Готель»). Програма читає записи про проживання гостей, перевіряє їх, обчислює показники й виконує запити Stream API. У ЛР № 5 дані зберігаються в параметризованому сховищі `Repository<T>`, експортуються у CSV універсальним рефлексивним експортером і читаються назад із перевіркою round-trip.

## Що додано в ЛР № 5
| Компонент | Призначення |
| :-- | :-- |
| `Repository<T>` | типобезпечне сховище: `add`, `all` (незмінний знімок), `find(Predicate<? super T>)`, `size` |
| `@CsvColumn` | анотація поля з назвою стовпця (`RUNTIME`, `FIELD`) |
| `CsvExporter` | універсальний експортер: знаходить поля з `@CsvColumn` рефлексією, екранує значення, пише UTF-8 |
| `CsvParser` | розбір CSV машиною станів (лапки, коми та перенесення рядка всередині полів) |
| `BookingRecord` | плоский запис варіанта: `room`, `guest`, `checkIn`, `checkOut`, `rate` |
| `BookingCsvReader` | читання CSV і відновлення `BookingRecord` |
| `DataStorageException` | власний checked-виняток із першопричиною (`getCause()`) |
| `Exporter<T>`, `JsonExporter` | завдання із зірочкою: другий формат через спільний інтерфейс |

`Main` тепер складає коректні бронювання в `Repository<Booking>` замість прямого списку. Експортер не містить назв класів предметної області: у тестах він експортує довільний клас `City`.

## Формат CSV
* Кодування UTF-8 під час запису й читання (`StandardCharsets.UTF_8`), без BOM.
* Перший рядок — заголовок із назв `@CsvColumn`. Порядок стовпців визначається сортуванням Java-імен полів (`checkIn`, `checkOut`, `guest`, `rate`, `room`):

```text
заїзд,виїзд,гість,тариф,номер
2026-01-01,2026-01-04,John Doe,150.0,101
2026-01-01,2026-01-08,"O'Neil, ""Shaq"" Jr.",220.5,205
```

* Поле з комою, лапкою або перенесенням рядка береться в подвійні лапки; лапка всередині подвоюється.
* Дати у форматі ISO `РРРР-ММ-ДД`, дробові числа з крапкою.
* Записи розділяються системним роздільником рядків; під час читання приймаються `\n`, `\r\n` і `\r`.
* Порожній файл і файл лише із заголовком дають порожній список записів.

| Поле | Тип | Стовпець | Інваріант |
| :-- | :-- | :-- | :-- |
| `room` | `int` | номер | більше нуля |
| `guest` | `String` | гість | не порожнє |
| `checkIn` | `LocalDate` | заїзд | задано |
| `checkOut` | `LocalDate` | виїзд | пізніше за заїзд |
| `rate` | `double` | тариф | додатне, скінченне |

Ключ запису: `room + checkIn`. Вхідний файл програми дат не містить, тому під час експорту заїзд дорівнює `2026-01-01`, а виїзд — заїзд плюс кількість ночей.

## Помилки
| Рівень | Приклад | Реакція |
| :-- | :-- | :-- |
| Модель | номер 0, виїзд не пізніше заїзду | `IllegalArgumentException` у конструкторі |
| Формат | не п'ять полів, незакриті лапки, інший заголовок | `DataStorageException` |
| Значення | хибне число або дата | `DataStorageException` із причиною `NumberFormatException` / `DateTimeParseException` |
| Файл | файла немає, шлях є каталогом | `DataStorageException` із `IOException` як причиною |
| Рефлексія | немає полів із `@CsvColumn`, поле недоступне | `DataStorageException` |

Помилки не поглинаються: `Main` друкує повідомлення й першопричину (`Export failed: ...`, `Cause: ...`).

## Формат входу
Файл у кодуванні UTF-8, один запис у рядку, поля розділено крапкою з комою (формат не змінився):

`room;guest;nights;nightlyRate;category`

| Поле | Тип | Перевірка |
| :-- | :-- | :-- |
| room | ціле число | більше нуля |
| guest | текст | не порожнє |
| nights | ціле число | більше нуля |
| nightlyRate | число з крапкою | більше нуля, скінченне (не NaN і не Infinity) |
| category | текст | не порожнє; Economy/Standard/Deluxe/Business → `StandardBooking`, Suite/Presidential/Penthouse → `PremiumBooking`, інше → `StandardBooking` (`RoomKind.OTHER`) |

Перший рядок із заголовком (містить слово `guest`) пропускається. Хибні рядки виводяться з номером і причиною та не впливають на результат.

## Показники та сумісність звіту
Файл звіту й чотири показники **не змінились** і збігаються з еталоном ЛР № 1 (`baseline/report-lab01.txt`). Блоки `Polymorphic details`, `Stream queries` і `CSV export` виводяться лише в консоль.

## Параметри командного рядка
* `--help` — вивести довідку;
* `--version` — показати версію програми (5.0.0);
* `--input FILE` — шлях до вхідного файла (за замовчуванням `data/input.csv`);
* `--output FILE` — шлях до файла звіту (за замовчуванням `out/report.txt`);
* `--date YYYY-MM-DD` — дата для запиту «активне на дату»;
* `--find-room N` — пошук бронювання за номером кімнати;
* `--export FILE` — експортувати бронювання у CSV, прочитати файл назад і порівняти записи;
* `--export-json FILE` — експортувати бронювання в JSON.

## Збірка і запуск
Windows (PowerShell), JDK 21:

```text
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd package
java "-Dstdout.encoding=UTF-8" -jar target\lab01-5.0.0.jar --input data/input-lab05.csv --output out/report.txt --export out/bookings.csv --export-json out/bookings.json
```

macOS та Ubuntu: `./mvnw test`, `./mvnw verify`, `./mvnw package`, `java -jar target/lab01-5.0.0.jar ...`.

Команда `verify` виконує компіляцію, 153 модульні тести JUnit 5 (84 із попередніх робіт + 69 нових), статичний аналіз SpotBugs і пакування виконуваного JAR. SpotBugs 4.9.7.0 не працює під JDK 27, тому `verify` запускається під JDK 21.

## Приклад (`data/input-lab05.csv`)
```text
--- CSV export ---
CSV saved to: out/bookings.csv
Header: заїзд,виїзд,гість,тариф,номер
Exported records: 6
Restored records: 6
Round-trip equal: true
JSON saved to: out/bookings.json
```

## Файли даних
* `data/input.csv` — контрольний вхід ЛР № 1–4 (12 записів);
* `data/input-lab03.csv`, `data/input-lab04.csv` — набори попередніх робіт;
* `data/input-lab05.csv` — набір ЛР № 5 (6 записів, один гість із комою та лапками, один кирилицею);
* `data/bookings-empty.csv` — CSV лише із заголовком;
* `data/bookings-broken.csv` — CSV із хибним числом у другому записі (для тесту помилки).

## CI
GitHub Actions (`.github/workflows/ci.yml`) запускає `./mvnw clean verify` на ubuntu-latest, windows-latest і macos-latest та публікує JAR як артефакт.

**Автор:** Денис Матіїшин, група КІ-303
