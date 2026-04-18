# Persistence - Przechowywanie Stanu Symulacji

## Cel

Simulator Service przechowuje stan sesji symulacji i historię kroków w PostgreSQL, aby umożliwić:
- wznowienie sesji po odświeżeniu (F5) lub reconnect,
- śledzenie postępu krok po kroku,
- odczyt historii wykonanych i pominiętych kroków.

## Model Danych

### `simulation_sessions`

Przechowuje metadane sesji:
- `id` (UUID jako String, klucz główny),
- `status` (`RUNNING`, `PAUSED`, `COMPLETED`, `CANCELLED`),
- `current_step`, `total_steps`, `total_recipes`,
- `message` (np. brak dostępnych przepisów),
- `created_at`, `updated_at`, `completed_at`.

### `simulation_steps`

Przechowuje historię kroków sesji:
- `id` (auto-generated),
- `session_id`,
- `step_number`,
- `recipe_id`, `recipe_name`, `preparation_time`,
- `status` (`PENDING`, `EXECUTED`, `SKIPPED`),
- `executed_at`, `created_at`.

## Repozytoria

- `SimulationSessionRepository`
- `SimulationStepRepository`

Repozytoria umożliwiają:
- odczyt i aktualizację stanu sesji,
- pobieranie historii kroków po `session_id`,
- wyznaczanie kolejnego kroku `PENDING` do wykonania.

## Recovery po Disconnect/F5

Klient korzysta z trwałego `sessionId`:
1. start sesji zwraca `sessionId`,
2. po reconnect klient wywołuje status/historię dla tego samego `sessionId`,
3. stan (`PAUSED`/`RUNNING`/`COMPLETED`) i wykonane kroki są odtwarzane z bazy.

## Przejścia Stanu

- `start` -> `RUNNING` (lub `COMPLETED`, gdy brak przepisów),
- `executeStep` -> aktualizacja kroku `PENDING` na `EXECUTED`,
- `pause`/`resume` -> zmiana `RUNNING <-> PAUSED`,
- `complete` -> `COMPLETED`, pozostałe kroki `PENDING` są oznaczane jako `SKIPPED`,
- `cancel` -> `CANCELLED`, pozostałe kroki `PENDING` są oznaczane jako `SKIPPED`.

Nieprawidłowe przejścia zwracają błąd `409 CONFLICT`.
