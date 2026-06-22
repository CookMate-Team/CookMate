# Dokumentacja Testowa Architektury CookMate

Niniejszy dokument opisuje ogólną strategię testowania dla całego ekosystemu CookMate. Pokazuje, w jaki sposób zaimplementowano testy w poszczególnych warstwach i podpowiada, w których plikach znajdują się kluczowe implementacje.

---

## 1. Testy Jednostkowe (Unit Tests)
**Cel:** Weryfikacja pojedynczych metod i klas (głównie logiki biznesowej – `Service`, mapowania obiektów – `Mapper`, czy obsługi błędów) w pełnej izolacji.
**Sposób wykonania:** Testy uruchamiane w pamięci używając **JUnit 5**. Zależności takie jak dostęp do bazy danych (repozytoria) czy połączenia zewnętrzne (klienci HTTP) są podmieniane za pomocą mocków biblioteką **Mockito**. 
**Sposób uruchomienia:**
```bash
# Uruchomienie testów dla całego projektu
mvn test

# Uruchomienie testów dla konkretnego mikroserwisu (np. main-service)
cd main-service
mvn test
```

| Mikroserwis | Co jest dokładnie testowane? | Przykładowe Odwołania do Plików (Ścieżki) |
|---|---|---|
| `main-service` | Logika obsługi przepisów (np. stronicowanie, rzucanie wyjątków przy braku danych). Transformacja struktury z TheMealDB na wewnętrzną bazę. Weryfikacja parsera składników (wyciąganie tekstu z formatu zewnętrznego) i asynchronicznego klienta. | `RecipeServiceTest.java`, `MealDbClientTest.java`, `StepMapperTest.java`, `GlobalExceptionHandlerTest.java` |
| `meal-planner-service` | Algorytmy deduplikacji listy zakupów (zliczanie i konsolidacja wielokrotnych wystąpień składnika). Walidacja losowania odpowiednich posiłków do slotów (np. BREAKFAST). | `MealPlanServiceTest.java`, `ShoppingListServiceTest.java` |
| `simulator-service` | Weryfikacja przesuwania stanu wewnątrz sesji symulacyjnej (czy zablokowane jest przejście, jeśli brakuje kroków). Weryfikacja wysyłania notyfikacji progresu do innych usług. | `SimulationServiceTest.java`, `SimulationSessionServiceTest.java` |
| `cooking-session-service` | Podstawowe filtry konfiguracyjne Spring Security – m.in. czy poprawnie zabezpieczono ścieżki i czy wpuszczają do Swaggera. | `SecurityFilterTest.java` |

---

## 2. Testy Integracyjne (Integration Tests)
**Cel:** Sprawdzenie współpracy wielu komponentów, w tym poprawnej konfiguracji kontekstu aplikacji, integracji z lokalną lub pamięciową bazą danych oraz poprawnego wstrzykiwania zależności sieciowych (np. Feign).
**Sposób wykonania:** Podnoszenie fragmentów kontekstu Springa za pomocą `@SpringBootTest`. Używanie np. `@WebMvcTest` do testowania samych kontrolerów REST z odciętą bazą, lub MockMvc do testowania całych przepływów (od żądania po zapis do bazy). Testowanie złącz (Endpoint -> Controller -> Service -> Repository).
**Sposób uruchomienia:**
Domyślnie testy integracyjne są uruchamiane razem z jednostkowymi przez Mavena.
```bash
mvn verify
```

| Mikroserwis | Co jest dokładnie testowane? | Przykładowe Odwołania do Plików (Ścieżki) |
|---|---|---|
| `main-service` | Symulacja zapytań HTTP na endpointy i weryfikacja poprawności formatów JSON i statusów, komunikacja klienta HTTP z wirtualnym API TheMealDB. | `DiscoveryIntegrationTest.java`, `RecipeControllerTest.java`, `DiscoveryControllerTest.java` |
| `simulator-service` | Przepływ procesu gotowania z przewodnikiem (guided cooking) wywoływanego przez endpointy. Integracja pomiędzy wywołaniem API a docelową reakcją klienta pobierającego. | `GuidedCookingFlowTest.java`, `SimulationControllerIntegrationTest.java` |
| `config-service` | Zdolność podniesienia centralnego serwera konfiguracji i załadowania plików YML. | `ConfigServiceIntegrationTest.java` |
| `gateway-service` | Filtry bezpieczeństwa - weryfikacja czy serwer autoryzacyjny i brama prawidłowo reagują na żądania bez tokenów (zwracając HTTP 401/403). | `SecurityConfigTest.java` |

---

## 3. Testy Akceptacyjne z wykorzystaniem (BDD / Cucumber)
**Cel:** Automatyczna weryfikacja czy aplikacja realizuje określone scenariusze biznesowe zgodnie z wymaganiami, czytelne testy stanowiące również żywą dokumentację (Living Documentation).
**Sposób wykonania:** Pisane w języku **Gherkin** (`Given/When/Then`) pliki `.feature`. Konfigurowane za pomocą biblioteki **Cucumber**, gdzie każdy krok w pliku opisowym mapowany jest na odpowiednią metodę Javową wykonującą kod testowy.
**Sposób uruchomienia:**
```bash
cd main-service
mvn test -Dtest=CucumberTest
```

| Mikroserwis | Co jest dokładnie testowane? | Przykładowe Odwołania do Plików (Ścieżki) |
|---|---|---|
| `main-service` | **Recipe Management**: Możliwość dodawania i wyszukiwania konkretnych przepisów użytkowników. **Recipe Search**: Weryfikacja działania integracji z TheMealDB (np. sprawdzanie filtrowania po składniku). | Gherkin: `src/test/resources/features/recipe-management.feature`, `recipe-search.feature`. Java: `CucumberTest.java` |

---

## 4. Testy Obciążeniowe i Wydajnościowe (Stress Tests / k6)
**Cel:** Sprawdzenie jak system radzi sobie przy równoległym obciążeniu przez setki wirtualnych użytkowników, detekcja tzw. wąskich gardeł (bottlenecks), określanie stabilności API, sprawdzanie czasów trwania procesu autoryzacji (SSO) i weryfikacja tolerancji na asynchroniczne pętle żądań.
**Sposób wykonania:** Użycie zewnętrznego środowiska i narzędzia **k6**. Scenariusze konfigurowane są za pomocą skryptów w języku **JavaScript**. K6 wysyła serię żądań i zbiera metryki (np. czas odpowiedzi na p95, stopień nieudanych żądań HTTP). Ustawia się progi weryfikacyjne (Thresholds), np. `http_req_duration: ['p(95)<500']` - co oznacza, że 95% żądań musi wrócić przed upływem 500ms.
**Sposób uruchomienia:**
Aplikacja CookMate musi najpierw być uruchomiona (np. przez `docker compose up -d`). Następnie:
```bash
# Używając lokalnie zainstalowanego k6:
k6 run stress_test/e2e_pipeline_test.js

# LUB używając kontenera Docker:
docker run --rm -i grafana/k6 run - < stress_test/e2e_pipeline_test.js
```

| Lokalizacja | Co jest dokładnie testowane? | Przykładowe Odwołania do Plików (Ścieżki) |
|---|---|---|
| Katalog: `stress_test/` | **Autoryzacja (Auth Flow)**: Złożone symulowanie wymiany tokenów, generowanie masowych zapytań wymagających poprawnego wczytywania kontekstu przez Keycloak i Gateway. | Skrypt: `auth_flow_test.js` |
| Katalog: `stress_test/` | **Testy Całościowe (E2E Pipeline)**: Bardzo zaawansowany proces, w którym Wirtualni Użytkownicy pobierają listę z `main-service`, wybierają dania, wysyłają prośbę do `meal-planner` a następnie generują z tego asynchronicznie listę zakupową. Testuje spójność systemu. | Skrypt: `e2e_pipeline_test.js` |
| Katalog: `stress_test/` | **Brama (Gateway)**: Surowy test mierzący szybkość przepustowości i limitów w Spring Cloud Gateway na bardzo proste zapytania pod obciążeniem. | Skrypt: `gateway_test.js` |

> Szczegółowy raport z przeprowadzonych, gotowych testów wydajnościowych k6 znajduje się w pliku `stress_test/system_testing_report.md`.

---

## 5. Statyczna Analiza Kodu i Linting (Static Analysis & Linting)
**Cel:** Automatyczna weryfikacja jakości kodu bez jego uruchamiania. Pozwala na wykrycie błędów składniowych (Bugs), luk bezpieczeństwa (Vulnerabilities), tzw. zapaszków w kodzie (Code Smells) oraz wymuszanie jednolitego formatowania i sprawdzanie poziomu pokrycia testami.
**Sposób wykonania:** 
- Na backendzie używany jest potężny silnik **SonarQube**, który skanuje pliki Javy przy użyciu pluginu Maven.
- Na frontendzie zastosowano bibliotekę **ESLint** i wsparcie TypeScript do typowania i rygorystycznego weryfikowania React'a (m.in. weryfikacja Hooków).
**Sposób uruchomienia:**

```bash
# Linting Frontendu (ESLint)
cd frontend
npm run lint

# Skanowanie całego kodu w SonarQube (wcześniej trzeba uruchomić platformę SonarQube w Dockerze)
# (Przykład wywołania na Windows po uruchomieniu lokalnego serwera na porcie 9000)
mvn clean verify sonar:sonar -Dsonar.projectKey=cookmate -Dsonar.host.url=http://localhost:9000 -Dsonar.login=<twój_token>
```

| Rodzaj | Co jest dokładnie testowane? | Przykładowe Odwołania do Plików (Ścieżki) |
|---|---|---|
| Backend & Frontend (SonarQube) | Wykrywanie Code Smells, podatności, powtarzalności kodu. Ocena jakości całego ekosystemu w przeglądarkowym panelu na porcie `9000`. | Dokumentacja: `docs/SonarQube.md` |
| Frontend (ESLint) | Style kodowania React, użycie hooków, poprawność TypeScript, zapobieganie błędom wykonania w przeglądarce. | Plik z regułami: `frontend/eslint.config.js` lub zdefiniowane w `package.json` (`npm run lint`, `npm run lint:sonar`). |
