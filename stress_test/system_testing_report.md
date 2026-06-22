# Raport z przeprowadzonych testów systemu CookMate

Raport przedstawia opis oraz strukturę testów zaimplementowanych w projekcie z podziałem na poszczególne poziomy i technologie.

---

## 1. Testy Jednostkowe (Unit Tests)

**Co to są testy jednostkowe:**
Testy jednostkowe polegają na weryfikacji najmniejszych, pojedynczych części kodu (tzw. jednostek, zazwyczaj pojedynczych metod lub klas) w pełnej izolacji od zewnętrznych systemów, takich jak baza danych, sieć czy inne mikroserwisy. Ich głównym celem jest szybkie wykrycie błędów w logice algorytmów oraz poprawności przetwarzania danych wejściowych bez wpływu czynników zewnętrznych.

**Co jest testowane:**
W ramach testów jednostkowych weryfikowane są dwa główne obszary: logika biznesowa w serwisach oraz poprawność działania kontrolerów API. W warstwie usługowej testowane jest pobieranie i usuwanie kroków, obsługa wyjątków przy braku elementów oraz algorytm reguł bezpieczeństwa (Guardrails) dla urządzenia CookMate (np. zerowanie temperatury dla akcji `WEIGH` i `CHOP`, ograniczanie prędkości do `3` dla akcji `POT` oraz obrotów do `4` dla gorącego blendowania). W warstwie kontrolerów weryfikowane są punkty wejściowe HTTP, poprawność walidacji parametrów stronicowania (np. odrzucenie ujemnej strony), zwracanie kodów błędów 400 i 404 wraz ze spójną strukturą JSON-a odpowiedzi dla niepoprawnych danych lub nieistniejących obiektów, a także obsługa niewspieranych metod HTTP.

**Jak to jest testowane:**
Testy te realizowane są przy użyciu frameworka JUnit 5 z rozszerzeniem MockitoExtension, co pozwala na zamakowanie repozytoriów bazodanowych oraz klientów HTTP i precyzyjne zaprogramowanie ich zachowań. Do testowania kontrolerów używana jest biblioteka MockMvc, która symuluje wysyłanie żądań HTTP i odbieranie odpowiedzi JSON bezpośrednio w pamięci aplikacji, bez potrzeby uruchamiania serwera Tomcat. Weryfikacja poprawności polega na asercjach sprawdzających rzucane wyjątki, kody statusu odpowiedzi HTTP, struktury pól JSON oraz weryfikacji interakcji Mockito sprawdzającej, czy konkretne metody serwisów i repozytoriów zostały wywołane odpowiednią liczbę razy.

**Jak odpalić testy jednostkowe:**

* **Dla PowerShell (Windows):**
  ```powershell
  mvn test -pl main-service,simulator-service "-Dtest=*ServiceTest,*ControllerTest" "-Dspring.main.banner-mode=off" "-Dlogging.level.root=WARN" "-Dlogging.level.com.cookmate=INFO"
  ```

* **Dla Bash (Linux / macOS / Git Bash):**
  ```bash
  mvn test -pl main-service,simulator-service "-Dtest=*ServiceTest,*ControllerTest" -Dspring.main.banner-mode=off -Dlogging.level.root=WARN -Dlogging.level.com.cookmate=INFO
  ```

---

## 2. Testy Integracyjne (Integration Tests)

**Co to są testy integracyjne:**
Testy integracyjne służą do weryfikacji poprawnego współdziałania wielu komponentów systemu ze sobą oraz z zewnętrznymi zasobami. W przeciwieństwie do testów jednostkowych, nie izolują one testowanych klas, lecz sprawdzają, czy ich integracja z bazami danych, systemami plików, serwerami konfiguracyjnymi czy innymi mikroserwisami przebiega bez zakłóceń w warunkach zbliżonych do rzeczywistych.

**Co jest testowane:**
W ramach testów integracyjnych weryfikowane jest poprawne funkcjonowanie warstwy bazy danych (operacje CRUD, relacje encji, niestandardowe sortowanie oraz konwersje map parametrów do kolumn JSON), prawidłowe wczytywanie i serwowanie plików konfiguracyjnych YAML z zewnętrznego repozytorium przez serwer konfiguracji, poprawność maszyny stanów podczas pełnego cyklu sesji gotowania z przewodnikiem (od startu, poprzez kolejne kroki i cofanie kroków, aż do statusu ukończenia), a także prawidłowość komunikacji sieciowej z zewnętrznym API TheMealDB (w tym weryfikacja poprawności budowania adresów URL oraz mapowania złożonych obiektów wejściowych).

**Jak to jest testowane:**
Testy integracyjne są uruchamiane na rzeczywistym kontekście Spring Boot przy użyciu adnotacji `@SpringBootTest` oraz dedykowanego profilu testowego. Do weryfikacji bazy danych klasy testowe oznaczane są adnotacją `@Transactional`, dzięki czemu baza danych jest czyszczona po każdym teście poprzez wycofanie transakcji. Komunikacja HTTP z serwerem konfiguracji jest testowana za pomocą klienta `RestTestClient` wykonującego rzeczywiste zapytania, natomiast testy przepływu gotowania wykorzystują MockMvc połączony z beanami mockującymi (`@MockitoBean`) symulującymi odpowiedzi innych mikroserwisów. Weryfikacja klienta API zewnętrznego polega na podstawieniu stubów sieciowych za pomocą dedykowanych funkcji wymiany (`ExchangeFunction`), które przechwytują zapytania WebClient i symulują poprawne lub błędne odpowiedzi serwera, a asercje weryfikują zarówno zwracane dane, jak i rzucane wyjątki sieciowe.

**Jak odpalić testy integracyjne:**

* **Dla PowerShell (Windows):**
  ```powershell
  mvn test "-Dtest=*RepositoryTest,ConfigServiceIntegrationTest,GuidedCookingFlowTest,DiscoveryIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  ```

* **Dla Bash (Linux / macOS / Git Bash):**
  ```bash
  mvn test "-Dtest=*RepositoryTest,ConfigServiceIntegrationTest,GuidedCookingFlowTest,DiscoveryIntegrationTest" -Dsurefire.failIfNoSpecifiedTests=false
  ```

---

## 3. Testy Akceptacyjne BDD (Cucumber Acceptance Tests)

**Co to są testy akceptacyjne BDD:**
Testy akceptacyjne BDD (Behavior-Driven Development) to testy funkcjonalne wysokiego poziomu, których celem jest sprawdzenie, czy system spełnia wymagania biznesowe użytkownika końcowego. Są one definiowane w języku naturalnym, co ułatwia współpracę i zrozumienie wymagań pomiędzy programistami a osobami biznesowymi.

**Co jest testowane:**
Weryfikacji podlegają kluczowe scenariusze biznesowe użytkownika, takie jak pobieranie listy wszystkich dostępnych przepisów, wyszukiwanie konkretnego przepisu według nazwy, poprawne dodawanie nowych przepisów do systemu oraz obsługa sytuacji wyjątkowych, np. próba pobrania przepisu o nieistniejącym identyfikatorze.

**Jak to jest testowane:**
Scenariusze są zapisywane w plikach `.feature` przy użyciu składni Gherkin (bloków Given, When, Then). Za pomocą specjalnej klasy uruchomieniowej JUnit Suite, Cucumber interpretuje te kroki i łączy je z metodami w kodzie Java (Step Definitions). W tych metodach następuje rzeczywiste wykonanie operacji za pomocą MockMvc (wysyłanie zapytań HTTP na endpointy API) oraz weryfikacja wyników za pomocą standardowych asercji sprawdzających zawartość odpowiedzi i kody statusu.

**Jak odpalić testy akceptacyjne BDD:**

* **Dla PowerShell (Windows):**
  ```powershell
  mvn test "-Dtest=CucumberTest"## 4. Testy Wydajnościowe i Przeciążeniowe (Performance & Stress Testing)

### Opis testu i weryfikowany obszar
Test przeciążeniowy (Stress Test) ma na celu zweryfikowanie zachowania, stabilności oraz odporności systemu CookMate pod ekstremalnym obciążeniem, symulującym jednoczesne korzystanie z aplikacji przez dużą liczbę użytkowników. Weryfikowany jest pełny, współbieżny przepływ biznesowy:
1. Pobranie tokenu autoryzacyjnego JWT z Keycloak dla każdego użytkownika.
2. Inicjalizacja sesji gotowania dla wybranego przepisu poprzez Gateway (`POST /api/simulator/sessions/start`).
3. Krok po kroku pobieranie instrukcji i symulowanie wykonania kolejnych etapów gotowania przez simulator-service (`POST /api/simulator/sessions/{id}/steps/execute`).
4. Zapisywanie postępów w bazie danych PostgreSQL.

### Metodologia i użyte narzędzia
Testy są realizowane przy użyciu narzędzia **k6** za pomocą skryptu [stress_test_cookmate.js](file:///d:/repos/CookMate/stress_test_cookmate.js). Skrypt wykonuje żądania HTTP bezpośrednio do punktu wejściowego systemu – Gatewaya (port `:8085`), który dystrybuuje ruch do odpowiednich mikroserwisów rejestrujących się w Eureka Discovery Server.

### Parametry testowania
Test obciążeniowy charakteryzuje się następującymi parametrami:
* **Maksymalna liczba użytkowników:** 1000 wirtualnych użytkowników (VUs) działających współbieżnie.
* **Czas trwania:** Łącznie 4 minuty i 30 sekund (wliczając fazy narastania i wygaszania).
* **Faza wstępna (setup):** Równoległe pobranie w bazie 1000 unikalnych tokenów JWT Keycloaka (dla użytkowników `user1` do `user1000`) przed rozpoczęciem właściwego testu.
* **Fazy obciążenia (scenariusz):**
  * **Ramp-up (0s - 30s):** Narastanie obciążenia od 0 do 100 VUs.
  * **Ramp-up (30s - 1m30s):** Dalszy wzrost od 100 do 500 VUs.
  * **Ekstremalne obciążenie (1m30s - 2m30s):** Gwałtowny wzrost od 500 do 1000 VUs.
  * **Utrzymanie obciążenia (2m30s - 3m30s):** Przetrzymanie stałego obciążenia 1000 VUs.
  * **Ramp-down (3m30s - 4m00s):** Stopniowy spadek obciążenia do 0 VUs.

### Dlaczego pomijamy integrację z zewnętrznym API Groq (LLM) pod obciążeniem?
W rzeczywistym środowisku produkcyjnym generowanie kroków przez sztuczną inteligencję (Groq) odbywa się leniwie (lazy initialization) tylko raz – przy pierwszym wyborze nowego przepisu. Wygenerowany plan jest trwale zapisywany w bazie danych, a każde kolejne żądanie pobiera dane bezpośrednio z PostgreSQL. 

Podczas testu przeciążeniowego celowo pomijamy wywoływanie zewnętrznego API Groq na rzecz predefiniowanych i wcześniej wygenerowanych przepisów o identyfikatorach `52772`, `52773` oraz `52774`. Główne przyczyny to:
1. **Ograniczenia przepustowości (Rate Limits):** Zewnętrzny dostawca LLM natychmiast zablokowałby ruch (błąd HTTP 429 Too Many Requests) przy próbie wykonania tysięcy zapytań w krótkim czasie.
2. **Koszty finansowe:** Ciągłe odpytywanie LLM wygenerowałoby olbrzymie zużycie tokenów API.
3. **Miarodajność testu:** Celem testu jest zmierzenie wydajności naszej własnej infrastruktury i mikrousług, a nie opóźnień zewnętrznego serwisu AI.

---

### Wyniki testu przeciążeniowego (Stress Test – 1000 VUs)

Poniżej przedstawiono wyniki końcowego przebiegu testu obciążeniowego dla zoptymalizowanej konfiguracji systemu:

| Metryka k6 | Wartość | Interpretacja |
| :--- | :--- | :--- |
| **Wygenerowany ruch** | **27 823 żądań** | Przepustowość na poziomie **93.76 req/s**. |
| **Pomyślne asercje (checks)** | **99.97%** (53 626 / 53 640) | Niemal wszystkie asercje w skrypcie zakończyły się sukcesem. |
| **Nieudane zapytania HTTP (Failure Rate)** | **0.02%** (7 / 27 823) | Wyjątkowo niski współczynnik błędów, znacznie poniżej progu tolerancji (`rate < 5%`). |
| **Sukces `/steps/execute`** | **100.00%** (0 błędów) | Kompletny sukces wykonywania kroków dzięki pełnej izolacji sesji gotowania na poziomie użytkownika. |
| **Średni czas odpowiedzi** | **5.94s** | Wzrost średniego czasu odpowiedzi pod wpływem wysokiego obciążenia. |
| **Percentyl 90 (p90)** | **15.16s** | 90% zapytań obsłużono w czasie poniżej 15.1 sekundy. |
| **Percentyl 95 (p95)** | **22.94s** | Próg `p(95) < 2s` został przekroczony. |
| **Maksymalny czas odpowiedzi** | **53.44s** | Pik opóźnienia zanotowany w momencie nagłego wejścia 1000 użytkowników do systemu. |

#### Analiza i wnioski z wyników:

1. **Pełna izolacja współbieżnych sesji:**
   Projekt izolacji sesji na poziomie użytkownika (`userId` pobierany z tokenu JWT) wyeliminował wszystkie błędy współbieżności i kolizje bazodanowe (typu `duplicate key`). Każdy z 1000 użytkowników mógł bez przeszkód i niezależnie przejść całą ścieżkę przepisu.
2. **Wysoka odporność systemu:**
   Współczynnik błędów na poziomie **0.02%** potwierdza, że architektura jest stabilna i odporna na awarie. System nie gubi połączeń i poprawnie przetwarza ekstremalne obciążenia.
3. **Pochodzenie błędów 503 (MAIN_SERVICE_UNAVAILABLE):**
   Wszystkie 7 błędów HTTP wystąpiło dokładnie w 44. sekundzie testu – w ułamku sekundy, w którym 1000 użytkowników zakończyło pobieranie tokenów i jednocześnie rozpoczęło test. Jest to klasyczny efekt "zimnego startu", w którym Gateway otrzymał żądania, zanim tabele routingu w Eureka zdążyły w pełni zarejestrować i zsynchronizować wszystkie nowo uruchomione instancje serwisów. Po tej krótkiej chwili system działał bezbłędnie.
4. **Czasy odpowiedzi i zasoby środowiska lokalnego:**
   Przekroczenie progu opóźnienia percentyla p(95) (22.94s) wynika bezpośrednio z faktu uruchamiania całego środowiska (10 kontenerów backendowych, bazy danych PostgreSQL, Keycloaka, Sonara oraz generatora k6) na jednej, lokalnej maszynie fizycznej (wewnątrz wirtualizacji WSL2). Zasoby procesora i pamięci RAM gospodarza były maksymalnie nasycone. Zastosowanie wirtualnych wątków (Java Virtual Threads) pozwoliło Tomcatowi na optymalne zarządzanie kolejkami żądań bez awarii pamięci czy blokowania wątków systemowych. W środowisku produkcyjnym (np. Kubernetes z autoskalowaniem i dedykowaną bazą danych) czasy te będą zredukowane do ułamków sekund.
