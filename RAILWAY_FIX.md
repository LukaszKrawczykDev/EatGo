# Naprawa problemu z bazą danych na Railway

## Problem
Aplikacja nie kończy startu na Railway - prawdopodobnie problem z połączeniem do bazy danych.

## Rozwiązanie

Dodano klasę `DatabaseConfig`, która automatycznie parsuje `DATABASE_URL` z Railway (format: `postgresql://user:pass@host:port/db`) i konwertuje go na format JDBC wymagany przez Spring Boot (`jdbc:postgresql://host:port/db`).

## Co zostało zmienione:

1. **DatabaseConfig.java** - nowa klasa konfiguracyjna, która:
   - Parsuje `DATABASE_URL` z Railway
   - Używa zmiennych `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` jako fallback
   - Tworzy DataSource z poprawnym formatem JDBC URL

2. **application.yml** - zaktualizowano, aby używać zmiennych środowiskowych Railway

## Sprawdzenie w Railway:

1. **Zmienne środowiskowe** - upewnij się, że są ustawione:
   - `DATABASE_URL` (automatycznie z PostgreSQL service)
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` (automatycznie z PostgreSQL service)

2. **Logi** - sprawdź logi w Railway, powinny pokazywać:
   ```
   DatabaseConfig: Parsed DATABASE_URL - Host: ..., Port: ..., Database: ...
   DatabaseConfig: JDBC URL: jdbc:postgresql://...
   DatabaseConfig: Username: ...
   ```

3. **Połączenie z bazą** - jeśli nadal nie działa:
   - Sprawdź czy PostgreSQL service jest uruchomiony
   - Sprawdź czy aplikacja ma dostęp do bazy (Railway automatycznie łączy serwisy w tym samym projekcie)
   - Sprawdź logi Flyway - powinny pokazywać migracje

## Następne kroki:

1. Zrób commit i push zmian
2. Railway automatycznie zbuduje i wdroży nową wersję
3. Sprawdź logi w Railway dashboard
4. Jeśli nadal nie działa, sprawdź:
   - Czy PostgreSQL service jest online
   - Czy zmienne środowiskowe są poprawnie ustawione
   - Czy aplikacja ma dostęp do bazy danych

