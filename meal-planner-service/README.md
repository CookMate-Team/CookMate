# Meal Planner Service

Serwis do generowania tygodniowych planów posiłków i list zakupów na podstawie danych z TheMealDB.

## Założenia

- nie przechowuje danych — wszystko generowane on-the-fly przy każdym żądaniu,
- dane o posiłkach i składnikach pobierane są z `main-service` (proxy do TheMealDB),
- plan tygodniowy jest losowy — każde wywołanie zwraca inny wynik,
- lista zakupów deduplikuje składniki po nazwie; zbiera wszystkie miary i przepisy dla każdego składnika,
- endpointy wymagają uwierzytelnienia JWT (Keycloak).

## Zależności

| Serwis | Rola |
|---|---|
| `main-service` | dostarcza kategorie, listę posiłków i szczegóły składników z TheMealDB |
| TheMealDB | zewnętrzne API (dostępne przez `main-service`) |
| Eureka (`discovery-service`) | rejestracja i wykrywanie serwisów |
| Keycloak | uwierzytelnianie JWT — każde żądanie wymaga Bearer tokena |

## Flow

1. Klient wysyła żądanie z tokenem JWT.
2. Serwis komunikuje się z `main-service` przez Feign Client.
3. Wynik jest budowany w pamięci i zwracany — brak zapisu do bazy.

Szczegółowy opis przepływów: [MEAL_PLANNING.md](MEAL_PLANNING.md)

## Endpointy

| Metoda | Endpoint | Body | Opis |
|---|---|---|---|
| GET | `/api/planner/weekly-plan?mealsPerDay={n}` | — | generuje plan na 7 dni; `n` od 1 do 5 |
| POST | `/api/planner/shopping-list` | `{ "mealIds": [...] }` | lista zakupów dla podanych posiłków |

## Przykładowe odpowiedzi

### GET /api/planner/weekly-plan?mealsPerDay=2

```json
{
  "days": [
    {
      "day": "Monday",
      "meals": [
        { "id": "52772", "name": "Teriyaki Chicken Casserole", "thumbnailUrl": "https://..." },
        { "id": "52884", "name": "Poutine", "thumbnailUrl": "https://..." }
      ]
    },
    {
      "day": "Tuesday",
      "meals": [...]
    }
  ]
}
```

### POST /api/planner/shopping-list

```json
// request
{
  "mealIds": ["52772", "52884"]
}

// response
{
  "items": [
    {
      "name": "Garlic",
      "measures": ["2 cloves", "1 clove"],
      "recipes": ["Teriyaki Chicken Casserole", "Poutine"]
    },
    {
      "name": "Soy Sauce",
      "measures": ["3/4 cup"],
      "recipes": ["Teriyaki Chicken Casserole"]
    }
  ]
}
```

## OpenAPI

- JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`
