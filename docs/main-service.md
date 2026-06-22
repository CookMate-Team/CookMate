# Main Service

## Przeznaczenie i Rola
`main-service` to główny mikroserwis domenowy, który odpowiada za zarządzanie danymi związanymi z przepisami, odkrywanie nowych potraw (integracja z zewnętrznym TheMealDB API) oraz korzystanie z zaawansowanych możliwości generowania kroków gotowania za pomocą sztucznej inteligencji.

## Kluczowe Funkcjonalności
* **Zarządzanie Przepisami (Recipe Management):** Dodawanie, aktualizowanie, pobieranie i usuwanie unikalnych przepisów użytkowników. Obsługa paginacji dla sprawnego wyświetlania na interfejsie.
* **Integracja z TheMealDB (Discovery):** Rozbudowane wyszukiwanie po nazwie, filtrowanie po składnikach, obszarach pochodzenia potrawy oraz pobieranie pełnych definicji z zewnętrznej bazy.
* **Generowanie Kroków Gotowania (Step Generation):** Komunikacja z usługą LLM (np. Groq) w celu transkrypcji ciągłego tekstu instrukcji na rozdzielone, iteracyjne kroki do gotowania. Wygenerowane kroki ułatwiają działanie sesji gotowania i symulatora.
* **Ulubione Przepisy (Favorite Recipes):** Dodawanie własnych oraz zewnętrznych potraw do listy ulubionych dla danego użytkownika z zachowaniem odpowiednich ról uwierzytelnienia.

## Integracje i Technologie
- Komunikacja bazy: Relacyjna baza PostgreSQL.
- Zabezpieczenia: Wymaga tokena dostępowego JWT z przypisaną rolą `ROLE_USER` (Keycloak).
- LLM Integration: Komunikacja zewnętrzna z endpointem Groq zdefiniowanym w `GroqClient`.

## Architektura i Przepływ Danych (Flow)

**Integracja TheMealDB (DiscoveryController)**
Zapytania (np. wyszukiwanie, filtrowanie) trafiają do `DiscoveryController`, który używa asynchronicznego `MealDbClient` opartego na `WebClient`. Dane z API zewnętrznego są bezpośrednio zwracane do klienta bez zapisu w bazie. Wyjątkiem jest odpytywanie o konkretny przepis po jego ID – jeśli użytkownik chce go edytować lub rozpocząć symulację, `RecipeService` konwertuje obiekt z TheMealDB (`Meal`) na lokalną encję `Recipe`, wykonując przy tym parsowanie i konsolidację składników.

**Zarządzanie Przepisami (RecipeController)**
Odpowiada za standardowe operacje CRUD na lokalnych przepisach użytkowników, z uwzględnieniem stronicowania z bazy (PostgreSQL).

**Generowanie Kroków (StepController)**
`StepService` pozwala wysłać zlecenie wygenerowania ustrukturyzowanych kroków przygotowania potrawy na bazie jej opisu przez model sztucznej inteligencji (Groq). Kroki te są następnie zapisywane i mogą zostać użyte przez *Simulator Service*.

Wszystkie błędy domenowe (np. `RECIPE_NOT_FOUND`) podlegają ujednoliconej globalnej obsłudze wyjątków (`GlobalExceptionHandler`), co gwarantuje stały format błędów `ApiErrorResponse`.

## Główne Biblioteki i Zastosowanie 
- **`spring-boot-starter-web` (Spring WebMVC)**: Używany do budowy tradycyjnych endpointów REST (np. `RecipeController`). Pozwala na obsługę zapytań HTTP w trybie blokującym (thread-per-request).
- **`spring-boot-starter-webflux` (WebClient)**: Mimo że projekt jest głównie oparty o Spring MVC, dodano paczkę WebFlux, aby móc używać `WebClient` w `MealDbClient`. Dzięki niemu zapytania do zewnętrznego API TheMealDB są wykonywane asynchronicznie (non-blocking HTTP client).
- **`spring-boot-starter-data-jpa`**: Używany do mapowania obiektów na relacyjną bazę danych (PostgreSQL). Pozwala na łatwe deklarowanie interfejsów (np. `RecipeRepository`), które automatycznie tłumaczą wywołania na zapytania SQL, obsługując operacje CRUD i paginację.
- **`springdoc-openapi-starter-webmvc-ui`**: Odpowiada za generowanie wizualnej dokumentacji API pod `/swagger-ui.html`. Skanuje adnotacje takie jak `@Tag` czy `@Operation` i wystawia plik OpenAPI, który ułatwia testowanie endpointów.
- **`spring-boot-starter-oauth2-resource-server` & `spring-boot-starter-security`**: Chronią endpointy wymagające logowania. Odpowiadają za parsowanie nadesłanego przez bramę (Gateway) tokenu JWT z formatu Keycloak i sprawdzanie, czy użytkownik posiada odpowiednie role i uprawnienia.
