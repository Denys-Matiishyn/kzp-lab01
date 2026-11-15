# Lab 3: Cross-platform Programming (Hotel Data Processing)

## Призначення
Консольний Java-застосунок для лабораторної роботи № 3 (варіант 14, «Готель»). Програма читає записи про проживання гостей із CSV-файла, перевіряє їх, відкидає хибні рядки з поясненням причини, обчислює показники та виводить звіт у консоль і текстовий файл. У ЛР № 3 клас-сутність `Booking` перетворено на ієрархію зі спадкуванням, enum, поліморфізмом і узгодженими `equals`/`hashCode`.

## Архітектурні зміни (Лабораторна робота № 3)
| Було (ЛР № 2) | Стало (ЛР № 3) |
| :-- | :-- |
| один клас `Booking`, категорія — довільний `String` | запечатаний абстрактний `Booking` + підтипи `StandardBooking`, `PremiumBooking` |
| вартість — єдина формула `nights × rate` | поліморфний метод `stayCost()` із різною реалізацією в підтипах |
| категорія — текст | `enum RoomKind` (підпис, ознака преміальності) |
| `Booking.fromCsv` створює один клас | `Booking.fromCsv` → `Booking.create` обирає підтип за категорією (єдине місце розгалуження) |
| рівності немає | узгоджені `equals`/`hashCode` (підтип + усі поля), перевірено в `HashSet`/`HashMap` |
| `List<Booking>` | `List<Booking>` із об'єктами різних підтипів, обробка без `if`/`switch` за підтипом |

* **Спадкування, а не композиція:** `StandardBooking` і `PremiumBooking` *є різновидом* бронювання, мають однакові поля й інваріанти, а відрізняються лише правилом вартості.
* **Базовий тип:** абстрактний клас (а не інтерфейс), бо підтипи мають спільний стан (`room`, `guest`, `nights`, `nightlyRate`, `category`) і спільну валідацію.
* **Завдання із зірочкою:** ієрархія `sealed`; `BookingDescriber.describeRule` — вичерпний `switch` зі зіставленням зі зразком без `default`.

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

## Правила вартості проживання (поліморфний метод `stayCost()`)
* `StandardBooking`: `nights × rate`; від 7 ночей — знижка 5 % (поле підтипу `longStayDiscount`, допустимо [0; 1)).
* `PremiumBooking`: `nights × rate × (1 + serviceCharge)`, сервісний збір 10 % за замовчуванням (поле підтипу `serviceCharge`, допустимо [0; 1]).

## Показники та сумісність звіту
Файл звіту й чотири показники **не змінились** і побайтово збігаються з еталоном ЛР № 1 (`baseline/report-lab01.txt`):

* кількість коректних записів;
* сумарна кількість ночей;
* загальний виторг (`nights × rate`, метод `getRevenue()`);
* найдовше проживання.

Нові поліморфні показники (кількість за рівнями, унікальні записи, загальна вартість за правилами підтипів) виводяться лише в консоль окремим блоком `--- Polymorphic details ---`.

## Параметри командного рядка
* `--help` — вивести довідку;
* `--version` — показати версію програми (3.0.0);
* `--input FILE` — шлях до вхідного CSV-файла (за замовчуванням `data/input.csv`);
* `--output FILE` — шлях до файла звіту (за замовчуванням `out/report.txt`); відсутні каталоги створюються автоматично.

## Збірка і запуск
Windows (PowerShell):

```text
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd package
java -jar target\lab01-3.0.0.jar --input data/input.csv --output out/report.txt
```

macOS та Ubuntu:

```text
./mvnw test
./mvnw verify
./mvnw package
java -jar target/lab01-3.0.0.jar --input data/input.csv --output out/report.txt
```

Команда `verify` виконує компіляцію, 40 модульних тестів JUnit 5 (9 із попередніх робіт + 31 нових), статичний аналіз SpotBugs і пакування виконуваного JAR.

## Файли даних
* `data/input.csv` — контрольний вхід ЛР № 1–2 (12 коректних записів);
* `data/input-lab03.csv` — приклад для ЛР № 3: по два коректні записи кожного підтипу й два хибні рядки.

## Приклад (`data/input-lab03.csv`)
```text
Row 6: Invalid nights count (must be > 0) -> 102;Clark Kent;-2;90.00;Economy
Row 7: Category is missing -> 500;Tony Stark;7;1500.00;

--- Final Results ---
Total valid records: 4
Total nights stayed: 19
Total revenue: $15050.00
Maximum nights by a single guest: 8

--- Polymorphic details ---
Premium bookings: 2
Standard bookings: 2
Unique bookings: 4
Total stay cost (tier rules): $16270.00
```

## CI
GitHub Actions (`.github/workflows/ci.yml`) запускає `./mvnw clean verify` на ubuntu-latest, windows-latest і macos-latest та публікує JAR як артефакт.

**Автор:** Денис Матіїшин, група КІ-303
