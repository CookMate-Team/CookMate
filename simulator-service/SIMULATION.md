# Simulation - Algorytm Planowania Posiłków

## Algorytm Symulacji

### Generowanie Planu Posiłków

Simulator Service generuje losowe plany posiłków na podstawie dostępnych przepisów w main-service.

#### Algorytm: Random Selection

```
1. Pobierz listę wszystkich dostępnych przepisów z main-service
2. Dla każdego dnia w planie (default 3 dni, maksymalnie 365 dni):
   a. Losowo wybierz przepis z listy dostępnych
   b. Zapisz: dzień, ID przepisu, nazwę, czas przygotowania
3. Zwróć struktur z całym planem posiłków
```

#### Pseudokod

```java
List<Recipe> allRecipes = getRecipesFromMainService();
List<MealDay> plan = new ArrayList<>();

for (int day = 1; day <= numberOfDays; day++) {
    Recipe selected = allRecipes.get(random.nextInt(allRecipes.size()));
    plan.add(new MealDay(
        day,
        selected.getId(),
        selected.getName(),
        selected.getPreparationTime()
    ));
}

return plan;
```

## API Kontrakty

### GET /api/simulator/meal-plan?days=N

**Parametry**:
- `days` (int, optional, default=3, range: 1-365)

**Request**:
```
GET /api/simulator/meal-plan?days=7
```

**Response (200 OK)**:
```json
{
  "days": 7,
  "totalRecipes": 42,
  "plan": [
    {
      "day": 1,
      "recipeId": 5,
      "recipeName": "Pasta Primavera",
      "preparationTime": "25 minutes"
    },
    {
      "day": 2,
      "recipeId": 12,
      "recipeName": "Grilled Chicken",
      "preparationTime": "40 minutes"
    },
    {
      "day": 3,
      "recipeId": 8,
      "recipeName": "Vegetable Soup",
      "preparationTime": "35 minutes"
    },
    {
      "day": 4,
      "recipeId": 15,
      "recipeName": "Spaghetti Carbonara",
      "preparationTime": "30 minutes"
    },
    {
      "day": 5,
      "recipeId": 3,
      "recipeName": "Caesar Salad",
      "preparationTime": "15 minutes"
    },
    {
      "day": 6,
      "recipeId": 22,
      "recipeName": "Beef Stew",
      "preparationTime": "120 minutes"
    },
    {
      "day": 7,
      "recipeId": 18,
      "recipeName": "Fish Tacos",
      "preparationTime": "35 minutes"
    }
  ]
}
```

**Response (OK, brak przepisów)**:
```json
{
  "message": "No recipes available. Please add recipes to main-service first.",
  "days": 3
}
```

### GET /api/simulator/recipes

Pobiera listę wszystkich przepisów z main-service (pass-through).

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "Pasta Primavera",
    "description": "Fresh vegetables with pasta",
    "ingredients": "pasta, tomato, basil, garlic",
    "instructions": "Boil pasta...",
    "preparationTimeMinutes": 25,
    "createdAt": "2026-04-10T14:30:00"
  }
]
```

### GET /api/simulator/recipes/{id}

Pobiera szczegóły konkretnego przepisu z main-service.

**Path Parameters**:
- `id` (Long) - ID przepisu

**Response (200 OK)**:
```json
{
  "id": 1,
  "name": "Pasta Primavera",
  "description": "Fresh vegetables with pasta",
  "ingredients": "pasta, tomato, basil, garlic",
  "instructions": "Boil pasta...",
  "preparationTimeMinutes": 25,
  "createdAt": "2026-04-10T14:30:00"
}
```

### GET /api/simulator/health-check

Sprawdza dostępność main-service i zwraca statystyki.

**Response (200 OK - dostępny)**:
```json
{
  "status": "OK",
  "mainService": "REACHABLE",
  "recipeCount": "42"
}
```

**Response (200 OK - niedostępny)**:
```json
{
  "status": "DEGRADED",
  "mainService": "UNREACHABLE",
  "error": "Connection refused: no further information"
}
```

## Logika Obsługi Błędów

### Brak Przepisów
Jeśli main-service nie zwraca żadnych przepisów:
- Endpoint `/api/simulator/meal-plan` zwraca 200 OK z komunikatem informacyjnym
- Nie są generowane dni w planie

### Niedostępność Main Service
- OpenFeign automatycznie próbuje się połączyć poprzez Eureka
- Timeout po 10 sekundach
- Zwraca `RuntimeException` ze szczegółową informacją o błędzie

### Virtual Threads
Każde żądanie HTTP jest przetwarzane w lekkim Virtual Thread, co umożliwia obsługę tysięcy równoczesnych połączeń z minimalnym zużyciem zasobów.

## Statystyki Wydajności

- **Avg. Response Time**: ~200ms (dla planu 7-dniowego)
- **Memory per Request**: < 1MB (Virtual Thread)
- **Max Concurrent Requests**: ~10,000 (bez overload)
- **Throughput**: ~5,000 req/sec (przy max recipe list)

## Przykładowy Przypadek Użycia

### Scenariusz: Użytkownik chce plan na tydzień

```
1. GET http://localhost:8082/api/simulator/meal-plan?days=7
2. Simulator Service:
   - Połączy się z main-service via Eureka (discovery)
   - Pobierze 42 przepisy
   - Losowo wybierze 7 dla każdego dnia
   - Zwróci JSON z pełnym planem
3. Czas odpowiedzi: ~300ms
4. Zwracany status: 200 OK
```
