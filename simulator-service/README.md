# Simulator Service

Lekki serwis do symulacji gotowania krok po kroku dla urządzenia wielofunkcyjnego.

## Założenia

- komunikacja z `main-service` jest prosta,
- sesja trzyma postęp i umożliwia wznowienie po awarii aplikacji,
- wykonanie kroku odbywa się przez jedno wywołanie endpointu,
- odpowiedź wykonania kroku zawiera tylko:
  - `stepNumber`
  - `success`

## Flow

1. `POST /api/simulator/sessions/start` z `recipeId`
2. Simulator pobiera kroki z `main-service` (`GET /api/recipes/{recipeId}/steps`) i zapisuje je jako `PENDING`
3. UI wykonuje kolejne kroki przez:
    - `POST /api/simulator/sessions/{sessionId}/steps/execute`
   - po każdym kroku symulator wysyła event do `cooking-session-service`:
     `POST /api/cooking-sessions/progress`
4. Po restarcie aplikacji UI pobiera:
   - `GET /api/simulator/sessions/{sessionId}/status`
   - `GET /api/simulator/sessions/{sessionId}/history`
5. Jeśli trzeba wrócić do etapu:
   - `POST /api/simulator/sessions/{sessionId}/rewind?stepNumber=N`

## Endpointy

| Metoda | Endpoint | Opis |
|---|---|---|
| POST | `/api/simulator/sessions/start` | start sesji dla `recipeId` |
| POST | `/api/simulator/sessions/{sessionId}/steps/execute` | wykonaj kolejny krok (one click) |
| POST | `/api/simulator/sessions/{sessionId}/step` | fallback: wykonanie konkretnego kroku przesłanego z zewnątrz |
| POST | `/api/simulator/sessions/{sessionId}/rewind?stepNumber=N` | cofnięcie/wznowienie od etapu |
| GET | `/api/simulator/sessions/{sessionId}/status` | status sesji |
| GET | `/api/simulator/sessions/{sessionId}/history` | historia kroków |

## Przykładowe odpowiedzi

### execute step

```json
{
  "stepNumber": 3,
  "success": true
}
```

## OpenAPI

- JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`
