# Plan wykonania #188

## Problem i cel
Wydzielenie mechanizmu sesji gotowania z `main-service` do nowego mikroserwisu `cooking-session-service`, zapewnienie komunikacji SSE z frontendem (bez pollingu), oraz aktualizacja symulatora i UI tak, aby sesja wznawiała się po powrocie na widok gotowania.

## Podejście
1. **Nowy mikroserwis `cooking-session-service`**
   - Utworzyć moduł Spring Boot (WebFlux + JPA) z rejestracją w Eureka i Config Server.
   - Dodać konfigurację w `config-repo` oraz wpis w `docker-compose.yml` (np. port 8083).
   - Ustawić osobny schemat bazy danych (`cooking_session`) w tej samej bazie `cookmate` i inicjalizację schematu przy starcie.

2. **Model i persistence sesji**
   - Przenieść i rozdzielić logikę z `SimulationProgress` do nowego serwisu: encje sesji (np. `CookingSession`) i zdarzeń/wykonań kroku.
   - Repozytoria dla: historii sesji, ostatniego kroku, aktywnej sesji po `recipeId`.
   - DTO kompatybilne z obecnym `StepCompletionEventDto`.

3. **API + SSE w `cooking-session-service` (spójne po `recipeId`)**
   - Założenie biznesowe: **jedna aktywna sesja na przepis**.
   - REST:
     - `POST /api/cooking-sessions/progress` – zapis eventu kroku z `simulator-service`
     - `GET /api/cooking-sessions/recipes/{recipeId}/history` – historia kroków
     - `GET /api/cooking-sessions/recipes/{recipeId}/latest` – ostatni krok
     - `GET /api/cooking-sessions/recipes/{recipeId}/active` – aktywna sesja do wznowienia
   - SSE:
     - `GET /api/cooking-sessions/recipes/{recipeId}/stream` (Flux + `Sinks.Many.multicast`) emitujący zdarzenia po zapisie kroku.

4. **Integracja `simulator-service`**
   - Zastąpić `MainServiceClient.notifyStepCompleted` klientem do `cooking-session-service`.
   - Skierować notyfikacje z wykonywania kroku na nowy endpoint `/progress`.
   - Zaktualizować `docker-compose.yml`, dodając env dla symulatora (np. `COOKING_SESSION_SERVICE_URL: http://cooking-session-service:8083`).
   - Zaktualizować testy oraz dokumentację symulatora.

5. **Frontend (SSE + wznowienie)**
   - Zastąpić polling w `GuidedCookingProvider` połączeniem SSE (`EventSource`) i aktualizacją stanu na eventach.
   - Przy starcie i przy wejściu na widok gotowania wywołać `/recipes/{recipeId}/active`, wznawiając sesję bez przechowywania `sessionId` w localStorage.
   - Dodać konfigurację URL dla nowego serwisu (env, `simulatorApi`).

6. **UI panelu symulatora**
   - Pokazywać tylko aktualny krok.
   - Dodać pasek postępu z odliczaniem czasu (**skala przyspieszona: 1 minuta przepisu = 1 sekunda rzeczywista**).
   - Przycisk „Next step” ma natychmiast przerywać timer i przechodzić dalej.

7. **Cleanup `main-service`**
   - Usunąć `SimulationProgress*` (model, repo, serwis, controller) oraz endpointy `/api/simulation-progress`.
   - Zaktualizować README / listę endpointów.

## Uwagi i decyzje
- **Baza danych:** ten sam PostgreSQL (`cookmate`) + osobny schemat `cooking_session`.
- **Brak pollingu:** SSE jest jedyną drogą aktualizacji kroku po stronie UI.
- **Wznowienie:** `/recipes/{recipeId}/active` zwraca aktywną sesję (RUNNING) i ostatni krok, aby UI mogło odtworzyć stan.
