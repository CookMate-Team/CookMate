Szczegółowa dokumentacja znajduje się w głównym katalogu projektu: [docs/simulator-service.md](../../docs/simulator-service.md)

## Tabele

### `simulation_sessions`

- `id` - identyfikator sesji
- `recipe_id` - identyfikator przepisu z main-service
- `status` - `RUNNING` / `COMPLETED`
- `current_step` - ostatni wykonany krok
- `total_steps` - liczba kroków w sesji
- `created_at`, `updated_at`, `completed_at`

### `simulation_steps`

- `session_id`
- `step_number`
- `recipe_name` (opis kroku)
- `preparation_time`
- `status` - `PENDING` / `EXECUTED`
- `executed_at`, `created_at`

## Recovery

- Po restarcie aplikacji sesja jest odtwarzana przez `sessionId`
- `status` + `history` pokazują dokładny etap
- `rewind` pozwala wrócić do zadanego kroku bez utraty sesji
