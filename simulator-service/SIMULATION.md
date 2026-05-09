# Simulation Flow

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
3. Koniec:
   - gdy nie ma już `PENDING`, sesja ma status `COMPLETED`
4. Recovery:
   - `GET /status` i `GET /history` odtwarzają postęp
   - `POST /rewind?stepNumber=N` ustawia sesję na wybrany etap
