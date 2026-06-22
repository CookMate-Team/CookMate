Szczegółowa dokumentacja znajduje się w głównym katalogu projektu: [docs/simulator-service.md](../../docs/simulator-service.md)

## Cel

Symulator wykonuje kroki gotowania sekwencyjnie (one-click), utrzymuje sesję i pozwala wrócić do etapu po awarii.

## Przebieg

1. Start sesji:
   - `POST /api/simulator/sessions/start` z `recipeId`
   - simulator pobiera listę kroków z `main-service`
   - kroki zapisuje jako `PENDING`
2. Wykonanie kroku:
    - `POST /api/simulator/sessions/{sessionId}/steps/execute`
    - pierwszy `PENDING` przechodzi na `EXECUTED`
    - odpowiedź: `{ stepNumber, success }`
    - notyfikacja postępu do `cooking-session-service`:
      `POST /api/cooking-sessions/progress`
3. Koniec:
   - gdy nie ma już `PENDING`, sesja ma status `COMPLETED`
4. Recovery:
   - `GET /status` i `GET /history` odtwarzają postęp
   - `POST /rewind?stepNumber=N` ustawia sesję na wybrany etap
