Szczegółowa dokumentacja znajduje się w głównym katalogu projektu: [docs/meal-planner-service.md](../../docs/meal-planner-service.md)

## Generowanie planu tygodniowego

### Algorytm oparty na slotach

Każdy dzień tygodnia jest budowany ze z góry określonych **slotów posiłków**, które zależą od parametru `mealsPerDay`. Slot determinuje kategorię, z której losowany jest jeden posiłek.

#### Mapowanie slotów na liczbę posiłków

| `mealsPerDay` | Sloty (w kolejności)                              |
|:---:|:----------------------------------------------------|
| 1   | MAIN                                               |
| 2   | BREAKFAST, MAIN                                    |
| 3   | BREAKFAST, MAIN, STARTER                           |
| 4   | BREAKFAST, MAIN, SIDE, DESSERT                     |
| 5   | BREAKFAST, MAIN, STARTER, SIDE, DESSERT            |

#### Mapowanie slotów na kategorie TheMealDB

| Slot      | Kategoria TheMealDB                                              |
|-----------|------------------------------------------------------------------|
| BREAKFAST | `Breakfast`                                                      |
| DESSERT   | `Dessert`                                                        |
| STARTER   | `Starter`                                                        |
| SIDE      | `Side`                                                           |
| MAIN      | losowa kategoria **spoza** {Breakfast, Dessert, Starter, Side}   |

Slot MAIN czerpie z puli kategorii zwróconych przez endpoint kategorii (np. Beef, Chicken, Seafood, Pasta, Lamb, Pork, Vegetarian), wykluczając cztery stałe kategorie. Dla każdego dnia pula jest tasowana, co zapewnia różnorodność dań głównych w ciągu tygodnia.

### Kroki algorytmu

1. Walidacja parametru `mealsPerDay` (1–5); wartości spoza zakresu → `IllegalArgumentException` → 400.
2. Wywołanie `GET /api/v1/discovery/categories` przez Feign Client (`MainServiceClient.getCategories()`).
   - Brak odpowiedzi / błąd sieci → `MainServiceCommunicationException` → 503.
   - Pusta lista kategorii → `MealPlanGenerationException` → 500.
3. Filtrowanie kategorii: kategorie stałe (Breakfast, Dessert, Starter, Side) są wykluczane z puli MAIN.
   - Brak kategorii głównych po filtrowaniu → `MealPlanGenerationException` → 500.
4. Dla każdego z 7 dni (poniedziałek–niedziela):
   - Tasowanie puli kategorii MAIN (zapewnia różnorodność dań głównych między dniami).
   - Dla każdego slotu z listy przypisanej do `mealsPerDay`:
     - MAIN → kategoria to pierwszy element potasowanej puli,
     - pozostałe sloty → stała kategoria z mapy slot→kategoria,
     - wywołanie `GET /api/v1/discovery/filter/category?c={category}` → lista posiłków,
     - losowanie jednego posiłku z listy,
     - jeśli kategoria nie ma posiłków → WARN w logach, slot jest pomijany.
5. Zwrot `WeeklyPlanResponse` z listą 7 obiektów `DayPlan`, każdy z polem `day` i listą `MealItem`.

### Diagram

```
getCategories()
     │
     ▼
filtruj kategorie MAIN (wyklucz: Breakfast, Dessert, Starter, Side)
     │
     ▼
dla każdego dnia (×7):
  tasuj pulę MAIN
  dla każdego slotu z listy dla mealsPerDay:
    wyznacz kategorię (stała lub losowa MAIN)
    getMealsByCategory(kategoria) → losuj 1 posiłek
     │
     ▼
WeeklyPlanResponse { days: [ DayPlan, ... ] }
```

---

## Generowanie listy zakupów

### Z podanych ID posiłków (`POST /api/planner/shopping-list`)

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
   - kolejne wystąpienie tego samego składnika:
     - miara dodawana do listy `measures`,
     - nazwa przepisu dodawana do listy `recipes` **tylko jeśli jeszcze jej tam nie ma**.
5. Zwrot `ShoppingListResponse` z listą `ShoppingListItem`:
   - `name` — nazwa składnika,
   - `measures` — wszystkie miary zebrane ze wszystkich przepisów,
   - `recipes` — unikalne nazwy przepisów, które używają tego składnika.

### Z zapisanego planu tygodniowego (`POST /api/planner/shopping-list/from-plan/{weeklyPlanId}`)

1. Weryfikacja właściciela: plan wyszukiwany po `weeklyPlanId` AND `userId` z JWT.
   - Brak planu lub plan należy do innego użytkownika → `WeeklyPlanNotFoundException` → 404.
2. Ekstrakcja listy `mealId` ze wszystkich encji `WeeklyPlanMeal` należących do planu.
3. Dalej jak w punkcie 2–5 powyżej (reużycie `ShoppingListService.buildShoppingList`).

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
