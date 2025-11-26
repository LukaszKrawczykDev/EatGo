# Deployment na Railway.app

## Krok 1: Przygotowanie projektu

Projekt jest już przygotowany z następującymi plikami:
- `Dockerfile` - definicja obrazu Docker
- `railway.json` - konfiguracja Railway
- `.railwayignore` - pliki do ignorowania podczas builda

## Krok 2: Utworzenie projektu na Railway

1. Zaloguj się na [railway.app](https://railway.app)
2. Kliknij **"New Project"**
3. Wybierz **"Deploy from GitHub repo"** (jeśli masz repo na GitHub) lub **"Empty Project"**

## Krok 3: Dodanie bazy danych PostgreSQL

1. W projekcie Railway kliknij **"+ New"**
2. Wybierz **"Database"** → **"Add PostgreSQL"**
3. Railway automatycznie utworzy bazę danych i ustawi zmienne środowiskowe:
   - `DATABASE_URL` - pełny connection string
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`

## Krok 4: Konfiguracja zmiennych środowiskowych

Railway automatycznie wykryje zmienne z bazy danych, ale możesz je również ustawić ręcznie:

1. W projekcie kliknij na serwis aplikacji
2. Przejdź do zakładki **"Variables"**
3. Upewnij się, że są ustawione:
   - `DATABASE_URL` (automatycznie z PostgreSQL service)
   - `DATABASE_USERNAME` (opcjonalnie, jeśli Railway używa innej konwencji)
   - `DATABASE_PASSWORD` (opcjonalnie)
   - `SPRING_PROFILES_ACTIVE=prod` (opcjonalnie, dla produkcji)
   - `PORT` (Railway automatycznie ustawia tę zmienną)

**Uwaga:** Railway automatycznie parsuje `DATABASE_URL` i tworzy zmienne `PGHOST`, `PGPORT`, itd. Jeśli potrzebujesz osobnych zmiennych, możesz je wyodrębnić z `DATABASE_URL`.

## Krok 5: Deploy aplikacji

### Opcja A: Deploy z GitHub

1. Połącz repozytorium GitHub z Railway
2. Railway automatycznie wykryje `Dockerfile` i zbuduje aplikację
3. Deploy uruchomi się automatycznie po push do głównej gałęzi

### Opcja B: Deploy z Railway CLI

```bash
# Zainstaluj Railway CLI
npm i -g @railway/cli

# Zaloguj się
railway login

# Połącz z projektem
railway link

# Deploy
railway up
```

## Krok 6: Konfiguracja portu

Railway automatycznie ustawia zmienną `PORT`. Aplikacja Spring Boot używa tej zmiennej dzięki konfiguracji w `application.yml`:
```yaml
server:
  port: ${PORT:8080}
```

## Krok 7: Sprawdzenie logów

1. W Railway dashboard kliknij na serwis aplikacji
2. Przejdź do zakładki **"Deployments"**
3. Kliknij na najnowszy deployment
4. Sprawdź **"Logs"** aby zobaczyć czy aplikacja uruchomiła się poprawnie

## Krok 8: Uzyskanie URL aplikacji

1. W Railway dashboard kliknij na serwis aplikacji
2. W zakładce **"Settings"** znajdź **"Generate Domain"**
3. Railway wygeneruje darmowy URL typu: `your-app-name.up.railway.app`
4. Możesz również dodać własną domenę w **"Custom Domain"**

## Rozwiązywanie problemów

### Problem: Aplikacja nie łączy się z bazą danych

**Rozwiązanie:**
- Sprawdź czy baza danych PostgreSQL jest uruchomiona
- Upewnij się, że zmienne środowiskowe są poprawnie ustawione
- Sprawdź logi aplikacji w Railway

### Problem: Port nie jest dostępny

**Rozwiązanie:**
- Railway automatycznie ustawia `PORT`, upewnij się że aplikacja używa `${PORT:8080}`
- Sprawdź czy aplikacja nasłuchuje na `0.0.0.0`, nie `localhost`

### Problem: Flyway migracje nie działają

**Rozwiązanie:**
- Sprawdź czy `DATABASE_URL` jest poprawnie ustawiony
- Upewnij się, że `DATABASE_USERNAME` i `DATABASE_PASSWORD` są ustawione (jeśli używasz osobnych zmiennych)
- Sprawdź logi Flyway w Railway

## Dodatkowe wskazówki

1. **Health Check:** Railway automatycznie sprawdza czy aplikacja odpowiada na porcie
2. **Auto Deploy:** Railway automatycznie deployuje po push do głównej gałęzi (jeśli połączysz GitHub)
3. **Environment Variables:** Wszystkie zmienne środowiskowe są szyfrowane i bezpieczne
4. **Logs:** Logi są dostępne w czasie rzeczywistym w Railway dashboard

## Koszty

Railway oferuje darmowy plan z:
- $5 darmowych kredytów miesięcznie
- Wystarczy na małą aplikację Spring Boot + PostgreSQL
- Płatność tylko za to, czego używasz

