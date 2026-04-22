# Simulation - Symulator Gotowania (Guided Cooking)

## Algorytm Symulacji Kroków Gotowania

Simulator Service odbiera kroki gotowania z main-service i symuluje ich wykonanie poprzez:
1. Wyświetlenie instrukcji kroku
2. Oczekiwanie na interakcję użytkownika
3. Odczekanie określonego czasu
4. Zwrócenie statusu "krok ukończony"

## Flow Komunikacji

```
┌──────────────────┐
│ Main Service     │
│ (Recipe Provider)│
└────────┬─────────┘
         │
         │ 1. Użytkownik wybiera "Guided Cooking"
         │ 2. Main-service tworzy sesję w simulator
         ▼
      POST /sessions/start
         │
         │ 3. Main-service przesyła krok #1
         ▼
  POST /sessions/{id}/step
      Step: {
        stepNumber: 1,
        description: "Podgrzej patelnię",
        durationSeconds: 5
      }
         │
         │ (Simulator odczekuje 5 sekund)
         │
         ▼
   RecipeStepResponseDto
      {
        completed: true,
        stepNumber: 1
      }
         │
         │ 4. Main-service przesyła krok #2
         ▼
  POST /sessions/{id}/step
    (kolejny krok)
         │
         └─► Powtarza się aż do końca przepisu
```

## API Kontrakty

### POST /api/simulator/sessions/start

Tworzy nową sesję gotowania.

**Request**: (puste ciało)

**Response (201 Created)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "currentStep": 0,
  "totalSteps": 0,
  "message": null,
  "history": []
}
```

### POST /api/simulator/sessions/{sessionId}/step

Odbiera krok gotowania i go przetwarza.

**Path Parameters**:
- `sessionId` (String) - ID sesji

**Request Body**:
```json
{
  "stepNumber": 1,
  "description": "Podgrzej patelnię do temperatury 180°C",
  "durationSeconds": 5,
  "temperature": "180°C",
  "weight": "200g",
  "additionalNotes": "Używaj oliwy z oliwek extra virgin"
}
```

**Response (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "stepNumber": 1,
  "completed": true,
  "status": "COMPLETED",
  "message": "Step completed successfully"
}
```

**Response (400 Bad Request - sesja nie w stanie RUNNING)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "stepNumber": 1,
  "completed": false,
  "status": "ERROR",
  "message": "Only RUNNING simulation can execute steps."
}
```

### GET /api/simulator/sessions/{sessionId}/status

Zwraca aktualny status sesji.

**Response (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "currentStep": 3,
  "totalSteps": 10,
  "message": null,
  "history": [
    {
      "stepNumber": 1,
      "recipeId": null,
      "recipeName": "Podgrzej patelnię do temperatury 180°C",
      "preparationTime": "5 seconds",
      "status": "EXECUTED",
      "executedAt": "2026-04-22T12:30:15.123"
    },
    {
      "stepNumber": 2,
      "recipeId": null,
      "recipeName": "Dodaj składniki",
      "preparationTime": "3 seconds",
      "status": "EXECUTED",
      "executedAt": "2026-04-22T12:30:21.456"
    },
    {
      "stepNumber": 3,
      "recipeId": null,
      "recipeName": "Mieszaj przez 2 minuty",
      "preparationTime": "120 seconds",
      "status": "EXECUTED",
      "executedAt": "2026-04-22T12:32:25.789"
    }
  ]
}
```

### POST /api/simulator/sessions/{sessionId}/pause

Pauzuje aktywną sesję.

**Response (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PAUSED",
  "currentStep": 3,
  "totalSteps": 10,
  "message": null,
  "history": [...]
}
```

### POST /api/simulator/sessions/{sessionId}/resume

Wznawia zapauzowaną sesję.

**Response (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "currentStep": 3,
  "totalSteps": 10,
  "message": null,
  "history": [...]
}
```

### POST /api/simulator/sessions/{sessionId}/complete

Kończy sesję (oznacza pozostałe kroki jako SKIPPED).

**Response (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "currentStep": 3,
  "totalSteps": 10,
  "message": null,
  "history": [...]
}
```

### POST /api/simulator/sessions/{sessionId}/cancel

Anuluje sesję.

**Response (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CANCELLED",
  "currentStep": 3,
  "totalSteps": 10,
  "message": null,
  "history": [...]
}
```

### GET /api/simulator/sessions/{sessionId}/history

Zwraca historię wszystkich kroków w sesji.

**Response (200 OK)**:
```json
[
  {
    "stepNumber": 1,
    "recipeId": null,
    "recipeName": "Podgrzej patelnię do temperatury 180°C",
    "preparationTime": "5 seconds",
    "status": "EXECUTED",
    "executedAt": "2026-04-22T12:30:15.123"
  },
  {
    "stepNumber": 2,
    "recipeId": null,
    "recipeName": "Dodaj składniki",
    "preparationTime": "3 seconds",
    "status": "EXECUTED",
    "executedAt": "2026-04-22T12:30:21.456"
  }
]
```

## Stany Sesji

- **RUNNING**: Sesja jest aktywna i może odbierać kroki
- **PAUSED**: Sesja jest zapauzowana, nie może odbierać nowych kroków
- **COMPLETED**: Sesja zakończona normalnie
- **CANCELLED**: Sesja anulowana przez użytkownika

## Stany Kroków

- **PENDING**: Krok czeka na wykonanie
- **EXECUTED**: Krok został wykonany
- **SKIPPED**: Krok został pominięty (sesja przerwana)

## Obsługa Błędów

### Sesja nie znaleziona
- Status: 404 Not Found
- Exception: `SimulationSessionNotFoundException`

### Nieprawidłowy stan sesji
- Status: 400 Bad Request
- Exception: `InvalidSimulationStateException`
- Przykłady:
  - Próba wykonania kroku na sesji w stanie PAUSED
  - Próba wznawiania sesji w stanie RUNNING

## Limitacje i Cechy

- **Virtual Threads**: Każde żądanie HTTP przetwarzane w lekkim virtual thread
- **Persistence**: Wszystkie sesje i kroki przechowywane w PostgreSQL
- **Concurrency**: Użycie lock'ów ensures thread-safety na sesję
- **Timeout**: Jeśli czekanie na krok przerwane, zwraca status INTERRUPTED

## Przykładowy Scenariusz Użytkownika

### Użytkownik gotuje Spaghetti Carbonara

```bash
# 1. Główna aplikacja tworzy sesję
POST /api/simulator/sessions/start
→ sessionId: "abc123"

# 2. Main-service przesyła krok 1
POST /api/simulator/sessions/abc123/step
{
  "stepNumber": 1,
  "description": "Zagotuj wodę w garnku (5L)",
  "durationSeconds": 10
}
→ completed: true

# 3. Main-service przesyła krok 2
POST /api/simulator/sessions/abc123/step
{
  "stepNumber": 2,
  "description": "Dodaj spaghetti do gotującej się wody",
  "durationSeconds": 3
}
→ completed: true

# 4. Main-service przesyła krok 3
POST /api/simulator/sessions/abc123/step
{
  "stepNumber": 3,
  "description": "Mieszaj przez 2 minuty",
  "durationSeconds": 120
}
→ (czeka 120 sekund) completed: true

# 5. Użytkownik może sprawdzić postęp
GET /api/simulator/sessions/abc123/status
→ currentStep: 3, totalSteps: 5, status: RUNNING

# 6. Użytkownik chce przerwać
POST /api/simulator/sessions/abc123/cancel
→ status: CANCELLED

# 7. Sprawdzenie historii
GET /api/simulator/sessions/abc123/history
→ [step1: EXECUTED, step2: EXECUTED, step3: EXECUTED, step4: SKIPPED, step5: SKIPPED]
```

## Techniczne Szczegóły

### Synchronizacja

Każda sesja ma własny lock, który zapobiega równoczesnym operacjom na tej samej sesji:
```java
synchronized (getExecutionLock(sessionId)) {
    // Przetwarzanie kroku
}
```

### Czekanie na Krok

Simulator czeka używając `Thread.sleep()`:
```java
Thread.sleep(stepDto.durationSeconds() * 1000L);
```

Jeśli wątek zostanie przerwany, zwraca status `INTERRUPTED`.

### Persistencja

Po każdej operacji sesja i kroki są zapisywane do bazy:
- Dodanie nowego kroku: `simulationStepRepository.save(step)`
- Zmiana statusu: `simulationSessionRepository.save(session)`
