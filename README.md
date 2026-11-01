# Lab 2: Cross-platform Programming (Hotel Data Processing)

## Призначення
Консольний Java-застосунок для лабораторної роботи № 2 (варіант 14, «Готель»). Програма читає записи про проживання гостей із CSV-файла, перевіряє їх, відкидає хибні рядки з поясненням причини, обчислює показники та виводить звіт у консоль і текстовий файл.
## Архітектурні зміни (Лабораторна робота № 2)
* **Клас-сутність `Booking`**: Стан об'єкта інкапсульовано у `private final` полях.
* **Валідація в конструкторі**: Перевірки інваріантів перенесено до конструктора класу `Booking`.
* **Фабричний метод**: Розбір CSV-рядка винесено у статичний метод `Booking.fromCsv(String)`.
* **Record**: Підсумкові показники згруповано у незмінний record `NightsRate`.
## Формат входу
Файл у кодуванні UTF-8, один запис у рядку, поля розділено крапкою з комою:

`room;guest;nights;nightlyRate;category`

| Поле | Тип | Перевірка |
| :-- | :-- | :-- |
| room | ціле число | більше нуля |
| guest | текст | не порожнє |
| nights | ціле число | більше нуля |
| nightlyRate | число з крапкою | більше нуля, скінченне (не NaN і не Infinity) |
| category | текст | не порожнє |

Перший рядок із заголовком (містить слово `guest`) пропускається. Хибні рядки виводяться з номером і причиною та не впливають на результат.

## Показники
* кількість коректних записів;
* сумарна кількість ночей;
* загальний виторг;
* найдовше проживання (максимальна кількість ночей одного запису).

## Параметри командного рядка
* `--help` — вивести довідку;
* `--version` — показати версію програми (1.0.0);
* `--input FILE` — шлях до вхідного CSV-файла (за замовчуванням `data/input.csv`);
* `--output FILE` — шлях до файла звіту (за замовчуванням `out/report.txt`); відсутні каталоги створюються автоматично.

## Збірка і запуск
Windows (PowerShell):

```text
.\mvnw.cmd clean verify
java -jar target/lab01-1.0.0.jar --input data/input.csv --output out/report.txt
```

macOS та Ubuntu:

```text
./mvnw clean verify
java -jar target/lab01-1.0.0.jar --input data/input.csv --output out/report.txt
```

Команда `verify` виконує компіляцію, 9 модульних тестів JUnit 5, статичний аналіз SpotBugs і пакування виконуваного JAR (`target/lab01-1.0.0.jar`).

## Приклад
Вхід (`data/input.csv`):

```text
room;guest;nights;nightlyRate;category
101;John Doe;3;150.00;Standard
202;Alice Smith;5;250.00;Deluxe
```

Вивід у консоль і файл:

```text
Total valid records: 2
Total nights stayed: 8
Total revenue: $1700.00
Maximum nights by a single guest: 5
```

## CI
GitHub Actions (`.github/workflows/ci.yml`) запускає `./mvnw clean verify` на ubuntu-latest, windows-latest і macos-latest та публікує JAR як артефакт.

**Автор:** Денис Матіїшин, група КІ-303