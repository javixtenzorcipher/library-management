# Library Desk

A standalone Java Swing desktop application for managing a small library. The formal, rubric-aligned, print-ready report is provided in [Library Desk Project Documentation - Print Ready](docs/Library%20Desk%20Project%20Documentation%20-%20Print%20Ready.docx).

## Features

- Dashboard showing catalog, available-copy, active-member, active-loan, and overdue-loan totals.
- Add, search, update, and delete books; manage member details and account status.
- Check out available books, review loan history, and record returns.
- Automatic 14-day due dates and overdue status.
- Local file persistence with business-rule validation for ISBNs, email addresses, copy counts, member status, and checkout limits.

## Requirements

- JDK 17 or later
- Windows, macOS, or Linux with a graphical desktop session
- No third-party libraries or database server

The application stores data in `.library-management/library-data.ser` under the current user's home directory.

## Run on Windows

Open PowerShell in the project root:

```powershell
$output = "out"
New-Item -ItemType Directory -Path $output -Force | Out-Null
$sources = Get-ChildItem "src\main\java" -Recurse -Filter "*.java" | ForEach-Object FullName
javac -encoding UTF-8 -d $output $sources
java -cp $output com.folio.library.Main
```

## Project structure

- `src\main\java\com\folio\library\domain` — encapsulated library records and shared abstract record type.
- `src\main\java\com\folio\library\service` — library workflows, validation, and business rules.
- `src\main\java\com\folio\library\storage` — repository abstraction and local file persistence.
- `src\main\java\com\folio\library\ui` — Swing application and screens.
- `src\test\java` — dependency-free service and GUI design checks.
- `screenshots` — maximized dashboard, catalog, Java search results, member directory, and loan history captures.
- `docs` — formal project report for submission.

The project demonstrates encapsulation through private domain fields and controlled methods; inheritance through `Book`, `Member`, and `Loan` extending `LibraryRecord`; polymorphism through subtype implementations of `getDisplayName()`; and abstraction through `LibraryRecord` and the `LibraryRepository` interface. The formal report explains the implementation and includes a UML overview, screenshots, and test evidence.

## Run tests

```powershell
$output = "out"
New-Item -ItemType Directory -Path $output -Force | Out-Null
$sources = Get-ChildItem "src" -Recurse -Filter "*.java" | ForEach-Object FullName
javac -encoding UTF-8 -d $output $sources
java -ea -cp $output com.folio.library.service.LibraryServiceTest
java -ea -cp $output com.folio.library.ui.LibraryUiDesignTest
```

## Regenerate full-screen screenshots

From a graphical desktop session, compile the project and run:

```powershell
$output = "out"
New-Item -ItemType Directory -Path $output -Force | Out-Null
$sources = Get-ChildItem "src" -Recurse -Filter "*.java" | ForEach-Object FullName
javac -encoding UTF-8 -d $output $sources
java -cp $output com.folio.library.ui.LibraryScreenshotGenerator screenshots
```

The generator uses temporary sample data and produces maximized screenshots of the dashboard, book catalog, Java search results, member directory, and loan history without changing the saved library data.
