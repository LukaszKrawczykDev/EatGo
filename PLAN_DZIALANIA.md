# Plan działania - EatGo (Uber Eats Clone)

## ✅ Zrealizowane

### 1. Backend API
- ✅ System autentykacji (JWT)
- ✅ Role: CLIENT, RESTAURANT_ADMIN, COURIER
- ✅ Endpointy REST dla wszystkich funkcjonalności
- ✅ System zamówień z workflow statusów
- ✅ System recenzji (restauracje i kurierzy)
- ✅ Powiązanie kurierów z restauracjami

### 2. Frontend - Strona główna
- ✅ Nowoczesny design z motywem dark/light
- ✅ Header z logo, logowaniem, przełącznikiem motywu
- ✅ Wybór miasta (ComboBox)
- ✅ Wyszukiwarka restauracji
- ✅ Lista restauracji z kartami
- ✅ Dialog z menu restauracji
- ✅ Animacje i responsywność
- ✅ Naprawione marginesy (padding 3rem)
- ✅ Kategorie restauracji z ikonami
- ✅ Geolokalizacja (przycisk "Użyj mojej lokalizacji")
- ✅ Filtrowanie restauracji po mieście i kategorii
- ✅ Obrazki restauracji zamiast ikon
- ✅ Modularny CSS (podział na pliki)

### 3. Frontend - Autentykacja
- ✅ Logowanie w formie modala (LoginDialog)
- ✅ Rejestracja w formie modala (RegisterDialog)
- ✅ Walidacja hasła (siła hasła, wymagania)
- ✅ Wybór typu konta przy rejestracji (RadioButtonGroup)
- ✅ Real-time walidacja hasła i potwierdzenia
- ✅ Przekierowanie po zalogowaniu/rejestracji na podstawie roli:
  - CLIENT → strona główna (`/`)
  - RESTAURANT_ADMIN → panel restauracji (`/restaurant`)
  - COURIER → panel kuriera (`/courier`)

### 4. Frontend - Menu użytkownika
- ✅ Dynamiczne menu użytkownika w headerze
- ✅ Przycisk powiadomień (tylko ikona)
- ✅ Przycisk koszyków z badge (wiele koszyków)
- ✅ Menu "Profil" z submenu (rozwija się po najechaniu):
  - Ustawienia
  - Adresy
  - Zamówienia
  - Wyloguj się
- ✅ Kolorystyka zgodna z motywem (gradient zamiast niebieskiego)
- ✅ Automatyczne przekierowanie na odpowiednią stronę po zalogowaniu

### 5. Frontend - Koszyk i zamówienia
- ✅ Koszyk zakupów (`CartView`) - wyświetlanie koszyków pogrupowanych po restauracjach
- ✅ Dodawanie dań do koszyka z menu i strony restauracji
- ✅ Edycja ilości dań w koszyku
- ✅ Usuwanie dań z koszyka
- ✅ Obliczanie sumy (cena dań + dostawa)
- ✅ Przechowywanie koszyka w localStorage (wiele koszyków - jeden per restauracja)
- ✅ Proces składania zamówienia (`CheckoutView`):
  - Wybór adresu dostawy (lista zapisanych adresów)
  - Dodawanie nowego adresu z walidacją
  - Wybór metody płatności (Gotówka/Karta/BLIK - symulacja)
  - Podsumowanie zamówienia z cenami
  - Integracja z `/api/orders` (POST)
  - Modal potwierdzenia zamówienia
- ✅ Historia zamówień (`OrdersView`):
  - Lista zamówień użytkownika
  - Podział na "W realizacji" i "Zakończone"
  - Szczegóły zamówienia (`OrderDetailsView`)
  - Integracja z `/api/orders` (GET)

### 6. Frontend - Strona restauracji
- ✅ Dedykowana strona restauracji (`RestaurantView`) - `/restaurant-view/{id}`
- ✅ Hero section z nazwą, adresem, ceną dostawy
- ✅ Menu z kategoriami - podział na kategorie
- ✅ Wyświetlanie dań z obrazkami
- ✅ Dodawanie dań do koszyka ze strony restauracji
- ✅ Modal menu (zachowany dla szybkiego podglądu)

### 7. Frontend - Zarządzanie adresami
- ✅ Strona zarządzania adresami (`AddressesView`) - `/addresses`
- ✅ Lista zapisanych adresów użytkownika
- ✅ Dodawanie nowego adresu (formularz z walidacją)
- ✅ Edycja adresu
- ✅ Usuwanie adresu
- ✅ Integracja z `/api/addresses`
- ✅ Pole na numer mieszkania/piętra

### 8. Frontend - Ustawienia użytkownika
- ✅ Strona ustawień (`SettingsView`) - `/settings`
- ✅ Zmiana hasła:
  - Walidacja siły hasła (wymagania, progress bar)
  - Wskaźnik siły hasła w czasie rzeczywistym
  - Integracja z `/api/auth/password` (PUT)
- ✅ Wybór domyślnego adresu dostawy (zapis w bazie danych)
- ✅ Wybór domyślnego miasta (zapis w bazie danych)
- ✅ Wybór motywu strony (light/dark) - zapis w bazie danych
- ✅ Automatyczne ładowanie ustawień z API przy wejściu
- ✅ Integracja z `/api/users/settings` (GET/PUT)

---

## 📋 Do zrealizowania

### Faza 1: Autentykacja i podstawowe widoki (PRIORYTET WYSOKI)

#### 1.1. Widok logowania (`LoginDialog`) ✅
- [x] Formularz logowania (email, hasło) - modal
- [x] Integracja z `AuthenticationService` (bezpośrednie wywołanie)
- [x] Przekierowanie po zalogowaniu:
  - CLIENT → strona główna z koszykiem
  - RESTAURANT_ADMIN → panel admina (`/restaurant`)
  - COURIER → panel kuriera (`/courier`)
- [x] Obsługa błędów (nieprawidłowe dane)
- [x] Link do rejestracji (przełączanie między dialogami)

#### 1.2. Widok rejestracji (`RegisterDialog`) ✅
- [x] Formularz rejestracji - modal:
  - Email, hasło, imię i nazwisko
  - Wybór roli (CLIENT lub RESTAURANT_ADMIN) - RadioButtonGroup
  - Dla RESTAURANT_ADMIN: nazwa restauracji, adres, cena dostawy
- [x] Integracja z `AuthenticationService` (bezpośrednie wywołanie)
- [x] Walidacja formularza (real-time)
- [x] Walidacja siły hasła (wymagania, progress bar)
- [x] Przekierowanie po rejestracji na podstawie roli
- [x] Link do logowania (przełączanie między dialogami)

#### 1.3. Zarządzanie sesją ✅
- [x] Przechowywanie tokena JWT w localStorage
- [x] Przechowywanie userId i roli w localStorage
- [x] Automatyczne przekierowanie na podstawie roli
- [x] Wylogowanie (usunięcie tokena)
- [x] Sprawdzanie ważności tokena - ✅ Zaimplementowane
- [x] Automatyczne dodawanie tokena do requestów API - ✅ Zaimplementowane

---

### Faza 2: Funkcjonalności klienta (PRIORYTET WYSOKI)

#### 2.1. Koszyk zakupów (`CartView` lub komponent) ✅
- [x] Dodawanie dań do koszyka z menu
- [x] Wyświetlanie zawartości koszyka (pogrupowane po restauracjach)
- [x] Edycja ilości dań
- [x] Usuwanie dań z koszyka
- [x] Obliczanie sumy (ceny dań + dostawa)
- [x] Przechowywanie koszyka w localStorage (wiele koszyków - jeden per restauracja)
- [x] Przycisk "Przejdź do zamówienia"

#### 2.2. Proces składania zamówienia (`CheckoutView`) ✅
- [x] Wybór adresu dostawy (lista zapisanych adresów użytkownika)
- [x] Możliwość dodania nowego adresu (z walidacją)
- [x] Automatyczne ładowanie domyślnego adresu z bazy danych
- [x] Wybór metody płatności (Gotówka/Karta/BLIK - symulacja)
- [x] Pola do wpisania kodu płatności (dla Karty i BLIK)
- [x] Podsumowanie zamówienia:
  - Lista dań z cenami jednostkowymi i całkowitymi
  - Cena dostawy
  - Suma całkowita
- [x] Integracja z `/api/orders` (POST)
- [x] Modal potwierdzenia zamówienia z szczegółami
- [x] Przekierowanie do widoku zamówień
- [ ] **Wybór czasu dostawy** - ASAP lub zaplanowana dostawa (jak w Uber Eats) - DO ZROBIENIA
- [ ] **Instrukcje dla kuriera** - opcjonalne notatki do dostawy - DO ZROBIENIA

#### 2.2.5. Strona restauracji (`RestaurantView`) - Wzorowane na Uber Eats ✅
- [x] **Dedykowana strona restauracji** (`/restaurant-view/{id}`) - pełny widok zamiast modala
- [x] **Hero section** - nazwa, adres, cena dostawy
- [x] **Menu z kategoriami** - podział na kategorie (np. Burgery, Pizza, Napoje)
- [x] **Wyświetlanie dań z obrazkami** - obrazy dań (danie1.jpg, danie2.jpg, etc.)
- [x] **Dodawanie do koszyka** - możliwość dodania dania do koszyka ze strony restauracji
- [x] Integracja z `/api/restaurants/{id}` i `/api/restaurants/{id}/menu`
- [x] Modal menu (zachowany dla szybkiego podglądu)
- [ ] **Filtrowanie menu** - po kategoriach, dostępności, wegetariańskie, itp. - DO ZROBIENIA
- [ ] **Szczegóły dania** - modal/dialog z pełnym opisem, składnikami - DO ZROBIENIA
- [ ] **Recenzje restauracji** - wyświetlanie recenzji klientów - DO ZROBIENIA
- [ ] **Informacje o restauracji** - godziny otwarcia, kontakt - DO ZROBIENIA
- [ ] **Dodawanie do ulubionych** - zapisywanie ulubionych restauracji - DO ZROBIENIA
- [ ] **Udostępnianie restauracji** - link do udostępnienia - DO ZROBIENIA

#### 2.3. Zarządzanie adresami (`AddressesView`) - Wzorowane na Uber Eats ✅
- [x] Lista zapisanych adresów użytkownika
- [x] Dodawanie nowego adresu (formularz z walidacją)
- [x] Pole na numer mieszkania/piętra
- [x] Edycja adresu
- [x] Usuwanie adresu
- [x] Integracja z `/api/addresses`
- [ ] **Walidacja zasięgu dostaw** - sprawdzanie czy adres jest w zasięgu (Warszawa, Lublin, Rzeszów) - DO ZROBIENIA
- [ ] **Geolokalizacja przy dodawaniu** - możliwość użycia aktualnej lokalizacji - DO ZROBIENIA
- [ ] **Mapa przy dodawaniu adresu** - wizualizacja lokalizacji (opcjonalnie) - DO ZROBIENIA
- [ ] **Komunikat jeśli adres poza zasięgiem** - informacja o dostępnych miastach - DO ZROBIENIA

#### 2.4. Historia zamówień (`OrdersView`) ✅
- [x] Lista zamówień użytkownika
- [x] Podział na "W realizacji" i "Zakończone"
- [x] Szczegóły zamówienia (`OrderDetailsView`):
  - Lista dań z cenami
  - Status zamówienia (z kolorowymi badge'ami)
  - Adres dostawy (z numerem mieszkania)
  - Data i czas zamówienia
  - Cena całkowita (subtotal + dostawa)
- [x] Integracja z `/api/orders` (GET)
- [ ] Możliwość śledzenia zamówienia - DO ZROBIENIA

#### 2.5. Recenzje (`ReviewsView` lub komponent)
- [ ] Formularz dodawania recenzji (po dostarczeniu)
- [ ] Ocena restauracji (1-5 gwiazdek)
- [ ] Ocena kuriera (1-5 gwiazdek)
- [ ] Tekst recenzji
- [ ] Integracja z `/api/reviews` (POST)
- [ ] Wyświetlanie recenzji na stronie restauracji

---

### Faza 3: Panel admina restauracji (PRIORYTET ŚREDNI)

#### 3.1. Dashboard restauracji (`RestaurantAdminView`) ✅ (Podstawa)
- [x] Utworzenie widoku (`/restaurant`)
- [x] Automatyczne przekierowanie RESTAURANT_ADMIN na `/restaurant`
- [ ] Statystyki:
  - Liczba zamówień dzisiaj/tygodniu
  - Przychód
  - Najpopularniejsze dania
- [ ] Szybki dostęp do funkcji:
  - Zarządzanie menu
  - Zarządzanie zamówieniami
  - Zarządzanie kurierami

#### 3.2. Zarządzanie menu (`MenuManagementView`)
- [ ] Lista dań restauracji
- [ ] Dodawanie nowego dania:
  - Nazwa, opis, cena
  - Dostępność (on/off)
- [ ] Edycja dania
- [ ] Usuwanie dania
- [ ] Integracja z `/api/restaurant/dishes`

#### 3.3. Zarządzanie zamówieniami (`RestaurantOrdersView`)
- [ ] Lista zamówień restauracji
- [ ] Filtrowanie po statusie
- [ ] Szczegóły zamówienia:
  - Lista dań
  - Dane klienta
  - Adres dostawy
- [ ] Zmiana statusu zamówienia:
  - ACCEPTED → COOKING → READY
  - Przypisanie kuriera (przy statusie READY)
- [ ] Integracja z `/api/restaurant/orders`

#### 3.4. Zarządzanie kurierami (`CouriersManagementView`)
- [ ] Lista kurierów restauracji
- [ ] Dodawanie kuriera (formularz rejestracji)
- [ ] Usuwanie kuriera
- [ ] Integracja z `/api/restaurant/couriers`

#### 3.5. Ustawienia restauracji (`RestaurantSettingsView`)
- [ ] Edycja danych restauracji:
  - Nazwa
  - Adres
  - Cena dostawy
- [ ] Integracja z `/api/restaurant` (PUT)

---

### Faza 4: Panel kuriera (PRIORYTET ŚREDNI)

#### 4.1. Dashboard kuriera (`CourierDashboardView`) ✅ (Podstawa)
- [x] Utworzenie widoku (`/courier`)
- [x] Automatyczne przekierowanie COURIER na `/courier`
- [ ] Lista przypisanych zamówień
- [ ] Filtrowanie po statusie (IN_DELIVERY)
- [ ] Szczegóły zamówienia:
  - Lista dań
  - Adres dostawy
  - Dane klienta
- [ ] Zmiana statusu na DELIVERED
- [ ] Integracja z `/api/courier/orders`

#### 4.2. Historia dostaw (`CourierHistoryView`)
- [ ] Lista zrealizowanych zamówień
- [ ] Statystyki (liczba dostaw, oceny)
- [ ] Integracja z `/api/courier/reviews`

---

### Faza 5: Ulepszenia i optymalizacja (PRIORYTET NISKI)

#### 5.1. Ulepszenia UX
- [ ] Loading states (spinner podczas ładowania)
- [ ] Error handling (komunikaty błędów)
- [ ] Success notifications
- [ ] Confirmation dialogs (przy usuwaniu)
- [ ] Breadcrumbs navigation
- [ ] Back button handling

#### 5.2. Ulepszenia strony głównej
- [ ] Kategorie/kuchnie (filtrowanie)
- [ ] Sortowanie (cena dostawy, nazwa)
- [ ] Featured restaurants (promowane)
- [ ] Obrazki restauracji (zamiast emoji)
- [ ] Oceny restauracji na kartach

#### 5.3. Optymalizacja
- [ ] Lazy loading dla list
- [ ] Paginacja dla zamówień
- [ ] Cache dla danych restauracji
- [ ] WebSocket dla real-time updates zamówień (opcjonalnie)

#### 5.4. Testy
- [ ] Testy jednostkowe dla serwisów
- [ ] Testy integracyjne dla endpointów
- [ ] Testy E2E dla głównych flow

---

## 💡 Dodatkowe funkcje wzorowane na Uber Eats

### Funkcjonalności klienta
- [ ] **Promocje i kody rabatowe** - system kodów promocyjnych przy zamówieniu
- [ ] **Program lojalnościowy** - punkty za zamówienia, nagrody
- [ ] **Powiadomienia push** - status zamówienia, promocje, nowe restauracje
- [ ] **Historia wyszukiwań** - szybki dostęp do ostatnio szukanych restauracji
- [ ] **Filtrowanie po diecie** - wegetariańskie, wegańskie, bezglutenowe, itp.
- [ ] **Szacowany czas dostawy** - dynamiczny czas na podstawie obciążenia restauracji
- [ ] **Śledzenie zamówienia w czasie rzeczywistym** - mapa z lokalizacją kuriera
- [ ] **Płatności online** - integracja z systemem płatności (opcjonalnie)
- [ ] **Zamówienia grupowe** - możliwość dzielenia koszyka z innymi użytkownikami
- [ ] **Zapisywanie ulubionych dań** - szybki dostęp do często zamawianych dań

### Funkcjonalności restauracji
- [ ] **Statystyki sprzedaży** - wykresy, trendy, analityka
- [ ] **Zarządzanie godzinami pracy** - różne godziny dla różnych dni
- [ ] **Zarządzanie dostępnością** - włączanie/wyłączanie przyjmowania zamówień
- [ ] **Szablony menu** - szybkie dodawanie podobnych dań
- [ ] **Zdjęcia dań** - upload i zarządzanie zdjęciami dań
- [ ] **Promocje restauracji** - tworzenie promocji, zniżek

### Funkcjonalności kuriera
- [ ] **Mapa z trasą** - nawigacja do klienta
- [ ] **Historia zarobków** - statystyki, wypłaty
- [ ] **Status dostępności** - włączanie/wyłączanie przyjmowania zleceń
- [ ] **Powiadomienia o nowych zamówieniach** - push notifications

---

## 🎯 Priorytetyzacja

### Najpierw (MVP):
1. ✅ Strona główna z listą restauracji
2. Logowanie i rejestracja
3. Koszyk i składanie zamówienia
4. Historia zamówień klienta
5. Panel admina - zarządzanie zamówieniami

### Potem:
6. Panel admina - zarządzanie menu
7. Panel kuriera
8. Recenzje
9. Zarządzanie adresami

### Na końcu:
10. Ulepszenia UX
11. Optymalizacja
12. Testy

---

## 📝 Uwagi techniczne

### Routing
- `/` - Strona główna (HomeView) - tylko dla CLIENT
- `/restaurant-view/{id}` - Strona restauracji (RestaurantView) - szczegóły, menu
- `/cart` - Koszyk (CartView)
- `/checkout/{restaurantId}` - Składanie zamówienia (CheckoutView)
- `/orders` - Historia zamówień (OrdersView)
- `/order/{id}` - Szczegóły zamówienia (OrderDetailsView)
- `/addresses` - Zarządzanie adresami (AddressesView)
- `/settings` - Ustawienia użytkownika (SettingsView)
- `/favorites` - Ulubione restauracje (FavoritesView) - opcjonalnie
- `/restaurant` - Panel admina (RestaurantAdminView) - tylko dla RESTAURANT_ADMIN
- `/courier` - Panel kuriera (CourierDashboardView) - tylko dla COURIER

### Security
- Wszystkie widoki wymagające autoryzacji powinny sprawdzać token JWT
- Przekierowanie do logowania jeśli brak tokena
- Różne widoki dla różnych ról

### State Management
- Token JWT w localStorage
- Koszyk w localStorage (wiele koszyków - jeden per restauracja)
- Wybrane miasto w localStorage (tymczasowe, dla niezalogowanych)
- **Ustawienia użytkownika w bazie danych:**
  - Domyślne miasto (`users.default_city`)
  - Domyślny adres dostawy (`users.default_address_id`)
  - Motyw strony (`users.theme`)
- Motyw w localStorage (dla kompatybilności z HeaderComponent)
- Ulubione restauracje w localStorage (opcjonalnie)

---

## 🚀 Następne kroki

1. ✅ **Faza 1 - Autentykacja** - Zakończona (logowanie/rejestracja w modalach, przekierowania)
2. ✅ **Faza 2.1-2.2** - Koszyk i zamówienia - Zakończona
3. ✅ **Faza 2.2.5** - Strona restauracji - Zakończona (podstawowa wersja)
4. ✅ **Faza 2.3** - Zarządzanie adresami - Zakończona (podstawowa wersja)
5. ✅ **Faza 2.4** - Historia zamówień klienta - Zakończona
6. ✅ **Faza 2.5** - Ustawienia użytkownika - Zakończona
7. **Faza 3** - Panel admina (rozwój dashboardu, zarządzanie menu, zamówieniami)
8. **Faza 4** - Panel kuriera (rozwój dashboardu)
9. **Faza 2.5** - Rozszerzenia (zaplanowane dostawy, instrukcje dla kuriera, walidacja zasięgu)

---

## 📌 Ostatnie zmiany (22.11.2025)

### Ustawienia użytkownika
- ✅ Utworzono `SettingsView` (`/settings`) z możliwością:
  - Zmiany hasła (z walidacją i wskaźnikiem siły)
  - Wyboru domyślnego adresu dostawy
  - Wyboru domyślnego miasta
  - Wyboru motywu strony (light/dark)
- ✅ Ustawienia zapisywane w bazie danych (nie w localStorage)
- ✅ Dodano pola do modelu `User`: `defaultCity`, `defaultAddress`, `theme`
- ✅ Utworzono migrację `V9__add_user_settings.sql`
- ✅ Dodano `UserService` z metodami `getUserSettings()` i `updateUserSettings()`
- ✅ Utworzono `UserController` z endpointami `/api/users/settings` (GET/PUT)
- ✅ `HomeView` ładuje domyślne miasto z API
- ✅ `CheckoutView` ładuje domyślny adres z API
- ✅ Dodano dostęp do `/api/users/**` w `SecurityConfig` dla roli CLIENT

### Koszyk i zamówienia
- ✅ Zaimplementowano pełny flow koszyka i składania zamówienia
- ✅ Modal potwierdzenia zamówienia z szczegółami
- ✅ Historia zamówień z podziałem na "W realizacji" i "Zakończone"
- ✅ Szczegóły zamówienia (`OrderDetailsView`)

### Strona restauracji
- ✅ Dedykowana strona restauracji (`RestaurantView`) z menu i obrazkami dań
- ✅ Dodawanie dań do koszyka ze strony restauracji

### Zarządzanie adresami
- ✅ Strona zarządzania adresami (`AddressesView`)
- ✅ Dodawanie, edycja, usuwanie adresów
- ✅ Pole na numer mieszkania/piętra

## 📌 Poprzednie zmiany (19.11.2025)

### UI/UX
- ✅ Zmieniono kolory hover na gradient (zamiast niebieskiego)
- ✅ Menu "Profil" rozwija się po najechaniu (hover), nie po kliknięciu
- ✅ Przycisk powiadomień jako ikona (bez tekstu)
- ✅ Kolorystyka zgodna z motywem aplikacji
- ✅ Przeładowanie strony po zalogowaniu/rejestracji (dla CLIENT)
- ✅ Wyświetlanie zapisanego miasta dla zalogowanych użytkowników + przycisk "Zmień miasto"

### Routing i logika aplikacji
- ✅ Strona główna (`/`) dostępna tylko dla CLIENT
- ✅ Utworzono `RestaurantAdminView` (`/restaurant`) - podstawowa struktura
- ✅ Utworzono `CourierDashboardView` (`/courier`) - podstawowa struktura
- ✅ Automatyczne przekierowanie po zalogowaniu/rejestracji na podstawie roli
- ✅ Sprawdzanie roli przy wejściu na stronę główną (przekierowanie jeśli nie CLIENT)
- ✅ Zapis wybranego miasta w localStorage
- ✅ Automatyczne przywracanie zapisanego miasta po zalogowaniu

### Plan rozwoju (nowe funkcje)
- 📋 Dedykowana strona restauracji (`/restaurant/{id}`) - wzorowana na Uber Eats
- 📋 Rozszerzone zarządzanie adresami z walidacją zasięgu dostaw
- 📋 Ulubione restauracje
- 📋 Zaplanowane dostawy
- 📋 Instrukcje dla kuriera przy zamówieniu

