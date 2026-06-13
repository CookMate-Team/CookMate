# Meal Planning Flow

## Generowanie planu tygodniowego

1. Walidacja parametru `mealsPerDay` (1–5); wartości spoza zakresu → `IllegalArgumentException` → 400.
2. Wywołanie `GET /api/v1/discovery/categories` przez Feign Client (`MainServiceClient.getCategories()`).
   - Brak odpowiedzi / błąd sieci → `MainServiceCommunicationException` → 503.
   - Pusta lista kategorii → `MealPlanGenerationException` → 500.
3. Dla każdego z 7 dni (poniedziałek–niedziela):
   - losowanie kategorii z pełnej listy (`Collections.shuffle` + pick),
   - wywołanie `GET /api/v1/discovery/filter/category?c={category}` → lista posiłków tej kategorii,
   - losowanie `mealsPerDay` posiłków z listy (bez powtórzeń w ramach dnia),
   - jeśli kategoria nie ma posiłków — dzień dostaje pustą listę (WARN w logach).
4. Zwrot `WeeklyPlanResponse` z listą 7 obiektów `DayPlan`, każdy z polem `day` i listą `MealItem`.

```
getCategories()
     │
     ▼
dla każdego dnia (×7):
  losuj kategorię → getMealsByCategory(category) → losuj n posiłków
     │
     ▼
WeeklyPlanResponse { days: [ DayPlan, ... ] }
```

## Generowanie listy zakupów

1. Klient przesyła `ShoppingListRequest` z listą `mealIds`.
2. Dla każdego `mealId`:
   - wywołanie `GET /api/v1/discovery/lookup/{id}` → `MealDetailListResponse`,
   - odpowiedź `null` lub brak listy `meals` → pominięcie bez błędu.
3. Ekstrakcja składników z każdego `MealDetailResponse`:
   - iteracja po slotach `strIngredient1`–`strIngredient20`,
   - slot `null` lub pusty (blank) → pominięcie,
   - odczyt odpowiadającej miary `strMeasure1`–`strMeasure20`.
4. Deduplicacja po nazwie składnika (case-sensitive, dokładne dopasowanie):
   - pierwsze wystąpienie → nowy wpis w mapie (`LinkedHashMap` zachowuje kolejność wstawienia),
   - kolejne wystąpienie tego samego składnika (z innego przepisu lub z innego slotu tego samego przepisu):
     - miara dodawana do listy `measures`,
     - nazwa przepisu dodawana do listy `recipes` **tylko jeśli jeszcze jej tam nie ma** (zapobiega duplikatom gdy TheMealDB wymienia ten sam składnik w dwóch slotach jednego przepisu).
5. Zwrot `ShoppingListResponse` z listą `ShoppingListItem`:
   - `name` — nazwa składnika,
   - `measures` — wszystkie miary zebrane ze wszystkich przepisów,
   - `recipes` — unikalne nazwy przepisów, które używają tego składnika.

```
mealIds: ["52772", "52884"]
     │
     ▼
dla każdego id:
  lookupById(id) → MealDetailResponse
     │
     ▼
dla każdego slotu strIngredient1–20:
  jeśli nie blank → dodaj do mapy (ingredient → accumulator)
     │             deduplicuj recipes, zbieraj measures
     ▼
ShoppingListResponse { items: [ ShoppingListItem, ... ] }
```
