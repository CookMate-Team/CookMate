# Main Service - Zarządzanie Przepisami

🔗 **Integracja z Zewnętrznym API**
- Wyszukiwanie posiłków z TheMealDB po pierwszej literze (a-z)
- Wyszukiwanie szczegółów posiłku po ID
- Automatyczne wyodrębnianie i formatowanie składników (do 20 składników)
- Asynchroniczne wywołania HTTP poprzez Spring WebClient

#### Obiekty Transferu Danych (Java Records)

**DTO dla Lokalnych Przepisów:**
- `RecipeDTO` - Pełna odpowiedź przepisu ze wszystkimi polami
- `RecipeCreateRequest` - Body żądania dla POST /api/recipes
- `RecipeUpdateRequest` - Body żądania dla PUT /api/recipes/{id}
- `RecipeListResponse` - Paginowana lista z metadanymi

**DTO dla TheMealDB:**
- `Meal` - Model posiłku z API TheMealDB (20 składników + wymiary, kategoria, kraj, instrukcje, tagi, link YouTube)
- `MealSearchResponse` - Wrapper odpowiedzi zawierający listę posiłków

#### Serwisy

**MealDbClient** (Integracja z Zewnętrznym API)
```
Lokalizacja: src/main/java/com/cookmate/main/service/MealDbClient.java

Odpowiedzialność:
- Komunikacja z API TheMealDB
- Obsługa reaktywnych (asynchronicznych) żądań HTTP poprzez WebClient
- Mapowanie odpowiedzi JSON na record Meal

Metody Publiczne:
- searchByLetter(String letter) → Mono<MealSearchResponse>
  * Wywołanie: GET https://www.themealdb.com/api/json/v1/1/search.php?f={letter}
  * Zwraca posiłki zaczynające się od danej litery (a-z)
  
- lookupById(String mealId) → Mono<MealSearchResponse>
  * Wywołanie: GET https://www.themealdb.com/api/json/v1/1/lookup.php?i={mealId}
  * Zwraca pełne szczegóły posiłku z 20 składnikami

Obsługa Błędów:
- Walidacja parametrów wejściowych (litera musi być znakiem, mealId nie może być pusty)
- Pakowanie błędów API w RuntimeException z opisowymi komunikatami
```

**RecipeService** (Logika Biznesowa)
```
Lokalizacja: src/main/java/com/cookmate/main/service/RecipeService.java

Lokalne Operacje CRUD:
- findAll() → List<Recipe>
- findById(Long id) → Optional<Recipe>
- findByName(String name) → List<Recipe>
- findPaginated(int page, int size) → RecipeListResponse
- save(RecipeCreateRequest) → Recipe
- update(Long id, Recipe) → Optional<Recipe>
- deleteById(Long id) → boolean

Integracja z Zewnętrznym API:
- searchMealsByLetter(String letter) → Mono<MealSearchResponse>
  * Delegowanie do MealDbClient
  
- lookupMeal(String mealId) → Mono<MealSearchResponse>
  * Delegowanie do MealDbClient
  
- syncMealFromTheMealDB(Meal meal) → Recipe
  * Konwersja posiłku z TheMealDB na lokalną encję Recipe
  * Wyodrębnianie kategorii, kraju i sformatowanych składników
  * Zapis do bazy danych

Metody Pomocnicze:
- buildIngredientsString(Meal) → String
  * Łączenie do 20 składników z wymiarami
  * Pomijanie pustych/null składników
  * Format: "składnik (wymiar), składnik2 (wymiar2), ..."
  
- addIngredient(StringBuilder, String, String)
  * Dodawanie pojedynczego składnika z walidacją
```

#### Kontrolery


#### Konfiguracja

**WebClientConfig**
```
Lokalizacja: src/main/java/com/cookmate/main/config/WebClientConfig.java

Bean: webClient() → WebClient
- Tworzy i konfiguruje Spring WebClient do żądań HTTP
- Używany przez MealDbClient do asynchronicznych wywołań API
- Część zależności Spring WebFlux
```

**GlobalExceptionHandler**
```
Lokalizacja: src/main/java/com/cookmate/main/exception/GlobalExceptionHandler.java

Obsługa:
- MethodArgumentNotValidException (HTTP 400)
  * Zwraca błędy walidacji na poziomie pól
  
- RuntimeException (HTTP 500)
  * Ogólna obsługa błędów
  
Odpowiedź: Record ErrorResponse z timestamp, status, message, details
```

### Przepływ Danych

```
Żądanie HTTP
    ↓
RecipeController / RecipeSearchController
    ↓
RecipeService
    ├─ Lokalne: RecipeRepository (JPA) → PostgreSQL
    └─ Zewnętrzne: MealDbClient → WebClient → TheMealDB API
    ↓
Odpowiedź JSON z DTO/Record
```

### Integracja TheMealDB

**Wyszukiwanie posiłków po pierwszej literze:**
```bash
GET /api/recipes/search/themealdb/letter?letter=a

Odpowiedź:
{
  "meals": [
    {
      "idMeal": "52771",
      "strMeal": "Arrabiata",
      "strCategory": "Owoce Morza",
      "strArea": "Włochy",
      "strInstructions": "Ugotuj pastę...",
      "strMealThumb": "https://www.themealdb.com/images/...",
      "strIngredient1": "spaghetti",
      "strMeasure1": "500g",
      "strIngredient2": "czosnek",
      "strMeasure2": "3 ząbki",
      ...
      "strIngredient20": null,
      "strMeasure20": null
    }
  ]
}
```

**Wyszukiwanie szczegółów posiłku po ID:**
```bash
GET /api/recipes/search/themealdb/meal?mealId=52772

Odpowiedź:
{
  "meals": [
    {
      "idMeal": "52772",
      "strMeal": "Teriyaki Chicken Casserole",
      "strCategory": "Kurczak",
      "strArea": "Japonia",
      "strInstructions": "Rozgrzej piekarnik do 350° F...",
      "strMealThumb": "https://...",
      "strTags": "Mięso,Zapiekanka",
      "strYoutube": "https://www.youtube.com/watch?v=...",
      "strIngredient1": "sos sojowy",
      "strMeasure1": "3/4 filiżanki",
      ...
    }
  ]
}
```

Globalna obsługa wyjątków zwraca spójne odpowiedzi:

```json
{
  "timestamp": "2026-04-16T15:48:00...",
  "status": 400,
  "message": "Walidacja nie powiodła się",
  "details": {
    "name": "Nazwa przepisu nie może być pusta",
    "ingredients": "Składniki nie mogą być puste"
  }
}

{
  "timestamp": "2026-04-16T15:48:00...",
  "status": 500,
  "message": "Błąd podczas wywoływania API TheMealDB: ...",
  "details": null
}
```


## Dokumentacja

### JavaDoc
Wszystkie publiczne klasy, metody i pola są udokumentowane komentarzami JavaDoc:
- Opis celu i zachowania
- Dokumentacja parametrów (@param)
- Dokumentacja wartości zwracanej (@return)
- Dokumentacja wyjątków (@throws)

Generowanie JavaDoc:
```bash
mvn javadoc:javadoc
```

### Dokumentacja API
Endpoints API są dokumentowane w klasach kontrolerów z komentarzami JavaDoc objaśniającymi:
- Wymagania dotyczące parametrów i body żądania
- Format i zawartość odpowiedzi
- Kody statusu HTTP
- Przypadki użycia i przykłady

### Referencja Zewnętrznych API
- **TheMealDB**: https://www.themealdb.com/api.php
  - Publiczne API, bez wymagania autentykacji

przykładowe wywołanie:
- curl https://www.themealdb.com/api/json/v1/1/search.php?f=a
- curl https://www.themealdb.com/api/json/v1/1/lookup.php?i=52772

## Struktura Projektu

```
main-service/
├── src/main/java/com/cookmate/main/
│   ├── MainServiceApplication.java
│   ├── config/
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   ├── RecipeController.java
│   │   └── RecipeSearchController.java
│   ├── dto/
│   │   ├── RecipeDTO.java
│   │   ├── RecipeCreateRequest.java
│   │   ├── RecipeUpdateRequest.java
│   │   ├── RecipeListResponse.java
│   │   ├── Meal.java
│   │   └── MealSearchResponse.java
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── model/
│   │   └── Recipe.java
│   ├── repository/
│   │   └── RecipeRepository.java
│   └── service/
│       ├── RecipeService.java
│       └── MealDbClient.java
├── src/main/resources/
│   └── application.yml
├── pom.xml
└── README.md
```
