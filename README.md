# Lab 4: Cross-platform Programming (Hotel Data Processing)

## Призначення
Консольний Java-застосунок для лабораторної роботи № 4 (варіант 14, «Готель»). Програма читає записи про проживання гостей із CSV-файла, перевіряє їх, відкидає хибні рядки з поясненням причини, обчислює показники та виводить звіт у консоль і текстовий файл. У ЛР № 4 циклові обчислення замінено запитами Stream API; зовнішній звіт не змінився.

## Що змінилось у ЛР № 4
| Було (ЛР № 3) | Стало (ЛР № 4) |
| :-- | :-- |
| цикл `for` у `Main` рахує суму ночей, виторг і максимум | `BookingQueries.summarize` (`mapToInt().sum()`, `mapToDouble().sum()`, `max()`) |
| цикл із `Map.merge` рахує бронювання за рівнями | `BookingQueries.countByTier` (`groupingBy` + `counting`) |
| `new HashSet<>(bookings).size()` | `BookingQueries.uniqueCount` (`distinct().count()`) |
| запитів до колекції немає | п'ять запитів варіанта, пошук через `Optional`, дворівневе групування |

Усі запити зібрано в класі `BookingQueries`. Кожен метод створює власний потік, не змінює вхідну колекцію, не форматує текст і не перевіряє підтип `Booking`. Форматування виконує `Main`.

## Запити Stream API (варіант 14)
| № | Метод | Конвеєр | Результат |
| :-- | :-- | :-- | :-- |
| 1 | `activeOn(bookings, checkIn, date)` | `stream → filter → toList` | `List<Booking>` |
| 2 | `roomNumbers(bookings)` | `stream → map → distinct → sorted → toList` | `List<Integer>` |
| 3 | `countByTariff(bookings)` | `stream → collect(groupingBy(getKind, EnumMap, counting))` | `Map<RoomKind, Long>` |
| 4 | `stayStatistics(bookings)` | `stream → collect(summarizingInt(getNights))` | `IntSummaryStatistics` |
| 5 | `longestStays(bookings, n)` | `stream → sorted(компаратор) → limit(n) → toList` | `List<Booking>` |
| пошук | `findByRoom(bookings, room)` | `stream → filter → findFirst` | `Optional<Booking>` |
| зірочка | `countByTierAndTariff(bookings)` | `groupingBy(tierLabel, TreeMap, groupingBy(getKind, EnumMap, counting))` | `Map<String, Map<RoomKind, Long>>` |

* **Активне на дату.** Вхідний CSV не містить дат заїзду й виїзду (формат не змінювався з ЛР № 1), тому дата заїзду передається параметром, а виїзд дорівнює заїзду плюс кількість ночей. Бронювання активне, якщо `checkIn <= date < checkIn + nights`: остання ніч ще активна, день виїзду вже ні. У програмі заїзд для всіх записів набору дорівнює `2026-01-01`.
* **Тариф** у групуванні — тарифна категорія номера `RoomKind`.
* **Компаратор top-N:** `comparingInt(getNights).reversed().thenComparingInt(getRoom)` — тривалість за спаданням, за рівної тривалості номер кімнати за зростанням. `reversed()` стоїть перед `thenComparingInt`, тому другий критерій не обертається.
* **Порожній вхід:** списки й карти порожні, `Optional.empty()`, статистика має `count = 0` і `average = 0.0` (у консолі друкується `Stay nights: no data`).

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
Файл звіту й чотири показники **не змінились** і збігаються з еталоном ЛР № 1 (`baseline/report-lab01.txt`): кількість коректних записів, сумарна кількість ночей, загальний виторг, найдовше проживання. Блоки `--- Polymorphic details ---` (ЛР № 3) і `--- Stream queries ---` (ЛР № 4) виводяться лише в консоль.

## Параметри командного рядка
* `--help` — вивести довідку;
* `--version` — показати версію програми (4.0.0);
* `--input FILE` — шлях до вхідного CSV-файла (за замовчуванням `data/input.csv`);
* `--output FILE` — шлях до файла звіту (за замовчуванням `out/report.txt`); відсутні каталоги створюються автоматично;
* `--date YYYY-MM-DD` — дата для запиту «активне на дату» (за замовчуванням `2026-01-03`);
* `--find-room N` — пошук бронювання за номером кімнати.

## Збірка і запуск
Windows (PowerShell), JDK 21:

```text
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd package
java -jar target\lab01-4.0.0.jar --input data/input-lab04.csv --output out/report.txt --find-room 404
```

macOS та Ubuntu: `./mvnw test`, `./mvnw verify`, `./mvnw package`, `java -jar target/lab01-4.0.0.jar ...`.

Команда `verify` виконує компіляцію, 84 модульні тести JUnit 5 (40 із попередніх робіт + 44 нові), статичний аналіз SpotBugs і пакування виконуваного JAR. SpotBugs 4.9.7.0 не працює під JDK 27, тому `verify` запускається під JDK 21.

## Файли даних
* `data/input.csv` — контрольний вхід ЛР № 1–3 (12 коректних записів);
* `data/input-lab03.csv` — приклад ЛР № 3 із двома хибними рядками;
* `data/input-lab04.csv` — набір ЛР № 4 (7 записів): дубль номера 101, рівна тривалість 7 ночей для перевірки другого критерію, запис на межі фільтра, два записи поза top-5.

## Приклад (`data/input-lab04.csv`, `--find-room 404`)
```text
--- Final Results ---
Total valid records: 7
Total nights stayed: 35
Total revenue: $21380.00
Maximum nights by a single guest: 10

--- Polymorphic details ---
Premium bookings: 2
Standard bookings: 5
Unique bookings: 7
Total stay cost (tier rules): $22903.00

--- Stream queries ---
Active on 2026-01-03 (check-in 2026-01-01): 5
Room numbers: 101, 102, 205, 210, 301, 404
Bookings by tariff: ECONOMY=1, STANDARD=2, DELUXE=1, BUSINESS=1, SUITE=1, PENTHOUSE=1
Stay nights: count=7, min=1, max=10, avg=5.00
Top 5 longest stays:
  room 210, Sarah Connor, 10 nights
  room 205, Diana Prince, 7 nights
  room 301, Dexter Morgan, 7 nights
  room 404, Tom Cruise, 5 nights
  room 101, John Doe, 3 nights
Search room 404: Tom Cruise (Penthouse), кімната 404: 5 ночей, 2000.00 грн
```

## CI
GitHub Actions (`.github/workflows/ci.yml`) запускає `./mvnw clean verify` на ubuntu-latest, windows-latest і macos-latest та публікує JAR як артефакт.

**Автор:** Денис Матіїшин, група КІ-303
