# Main Service - Zarządzanie Przepisami

**Integracja z Zewnętrznym API**
- Wyszukiwanie szczegółów posiłku po ID
- Wyszukiwanie po nazwie: Umożliwia odnalezienie potraw na podstawie frazy
- Filtrowanie po składniku: Możliwość listowania wszystkich dań, w których dany produkt jest składnikiem głównym.
- Dynamiczne listy: Pobieranie aktualnych list kategorii, obszarów (kuchni świata) oraz wszystkich dostępnych składników
- Automatyczne wyodrębnianie i formatowanie składników (do 20 składników)
- Asynchroniczne wywołania HTTP poprzez Spring WebClient
- Reaktywne typy danych: Serwis operuje na obiektach Mono i Flux, co ułatwia dalszą integrację z frontendem lub innymi mikroserwisami.

#### Obiekty Transferu Danych (Java Records)

**DTO dla Lokalnych Przepisów:**
- `RecipeDTO` - Pełna odpowiedź przepisu ze wszystkimi polami
- `RecipeCreateRequest` - Body żądania dla POST /api/recipes
- `RecipeUpdateRequest` - Body żądania dla PUT /api/recipes/{id}
- `RecipeListResponse` - Paginowana lista z metadanymi
- `StepDTO` – Reprezentuje pojedynczy krok w procesie przygotowania dania. Zawiera numer kroku, opis, typ akcji oraz czas trwania.
- 
**DTO dla TheMealDB:**
- `Meal` - Model posiłku z API TheMealDB (20 składników + wymiary, kategoria, kraj, instrukcje, tagi, link YouTube)
- `MealSearchResponse` - Wrapper odpowiedzi zawierający listę posiłków
- `MealSearchResponse` – Kontener dla listy obiektów Meal zwracanych przez wyszukiwarkę i endpointy filtrujące.
- `CategoryResponse` – Zawiera pełną listę kategorii wraz z ich opisami i linkami do miniatur graficznych (idCategory, strCategory, strCategoryThumb, strCategoryDescription).
- `CommonListResponse` – Uniwersalny rekord do obsługi list słownikowych (Area, Ingredients, Categories). 
- 
#### Serwisy
**MealDbClient** (Integracja z Zewnętrznym API)
```
Lokalizacja: src/main/java/com/cookmate/main/service/MealDbClient.java

Odpowiedzialność:
- Komunikacja z API TheMealDB przy użyciu stosu reaktywnego.
- Obsługa asynchronicznych żądań HTTP poprzez WebClient (non-blocking).
- Mapowanie surowych odpowiedzi JSON na ujednolicone rekordy Java (Meal, Category, CommonList).

Metody Publiczne:
- searchByName(String name) → Mono<MealSearchResponse>
  * Wywołanie: GET /search.php?s={name}
  * Zwraca listę posiłków pasujących do nazwy.
- lookupById(String mealId) → Mono<MealSearchResponse>
  * Wywołanie: GET /lookup.php?i={mealId}
  * Pobiera pełne szczegóły dania, w tym 20 pól składników.
- filterByIngredient(String ingredient) → Mono<MealSearchResponse>
  * Wywołanie: GET /filter.php?i={ingredient}
  * Filtruje przepisy według głównego składnika.
- listFullCategories() → Mono<CategoryResponse>
  * Wywołanie: GET /categories.php
  * Zwraca rozszerzone informacje o kategoriach (zdjęcia, opisy).
- listAllBy(String type) → Mono<CommonListResponse>
  * Wywołanie: GET /list.php?{type}=list
  * Pobiera słowniki: 'a' (kuchnie świata), 'i' (składniki), 'c' (kategorie).

Obsługa Błędów:
- Centralizacja zapytań w prywatnej metodzie fetch().
- Reaktywna obsługa błędów (doOnError) i pakowanie ich w RuntimeException z opisem.
```

**RecipeService** (Logika Biznesowa)
```
Lokalizacja: src/main/java/com/cookmate/main/service/RecipeService.java

Lokalne Operacje CRUD (Baza Danych):
- findAll() → List<Recipe>
- findById(Long id) → Optional<Recipe>
- findByName(String name) → List<Recipe> (wyszukiwanie ignore-case w bazie lokalnej).
- findPaginated(int page, int size) → RecipeListResponse (paginacja Spring Data JPA).
- save(RecipeCreateRequest) → Recipe.
- update(Long id, Recipe) → Optional<Recipe> (pełna aktualizacja encji).
- deleteById(Long id) → boolean.

Integracja z Discovery API (Delegacja):
- searchMealsByName(String name) → Mono<MealSearchResponse>.
- lookupMeal(String mealId) → Mono<MealSearchResponse>.
- filterByIngredient(String ingredient) → Mono<MealSearchResponse>.
- getAllCategories() → Mono<CategoryResponse>.
- getDictionaryList(String type) → Mono<CommonListResponse>.

Synchronizacja i Transformacja:
- syncMealFromTheMealDB(Meal meal) → Recipe
  * Konwersja obiektu Meal (Discovery) na lokalną encję Recipe.
  * Agregacja metadanych (kategoria, kraj) do opisu.
  * Uruchomienie parsera składników i zapis do bazy.

Metody Pomocnicze:
- buildIngredientsString(Meal) → String
  * Automatyczne wyodrębnianie danych z 20 par pól (strIngredientX + strMeasureX).
  * Tworzenie jednego, sformatowanego ciągu znaków dla bazy danych.
- addIngredient(StringBuilder, String, String)
  * Walidacja null/empty dla każdego z 20 składników.
  * Formatowanie: "Nazwa (Miara)" z poprawną interpunkcją.
- toDTO(Recipe) → RecipeDTO
   * Mapowanie encji JPA na niemodyfikowalny rekord wyjściowy.
```

**StepService** (Logika kroków przepisu)
```
Lokalizacja: src/main/java/com/cookmate/main/service/StepService.java

Metody Publiczne:
- getStep(Long stepId) → StepDTO
  * Pobiera pojedynczy krok na podstawie ID.
  * Dla nieistniejącego kroku rzuca StepNotFoundException.
```

#### Kontrolery
**RecipeController**
```
Lokalizacja: src/main/java/com/cookmate/main/controller/RecipeController.java
Endpoint bazowy: /api/recipes

Odpowiedzialność:
- Obsługa operacji CRUD na przepisach zapisanych w lokalnej bazie danych.
- Walidacja danych wejściowych (Bean Validation).

Metody:
- GET / – Pobieranie wszystkich przepisów lub wyszukiwanie po nazwie (?name=).
- GET /paginated – Paginowana lista przepisów z metadanymi (?page=0&size=10).
- GET /{id} – Pobieranie szczegółów konkretnego przepisu.
- POST / – Tworzenie nowego przepisu na podstawie RecipeCreateRequest.
- PUT /{id} – Aktualizacja istniejącego przepisu (RecipeUpdateRequest).
- DELETE /{id} – Usuwanie przepisu z bazy danych.
```
**DiscoveryController**
```
Lokalizacja: src/main/java/com/cookmate/main/controller/DiscoveryController.java
Endpoint bazowy: /api/v1/discovery

Odpowiedzialność:
- Udostępnianie danych z TheMealDB w sposób asynchroniczny (WebFlux).
- Zastępuje przestarzały RecipeSearchController.

Metody:
- GET /search – Wyszukiwanie potraw po nazwie (?name=Arrabiata).
- GET /lookup/{id} – Pobieranie kompletnych danych potrawy z API zewnętrznego.
- GET /filter/ingredient – Filtrowanie po głównym składniku (?i=chicken_breast).
- GET /categories – Pobieranie pełnej listy kategorii z opisami i zdjęciami.
- GET /list – Pobieranie słowników pomocniczych (?type=a|i|c).
```

**StepController**
```
Lokalizacja: src/main/java/com/cookmate/main/controller/StepController.java
Endpoint bazowy: /steps

Metody:
- GET /{stepId} – Pobieranie pojedynczego kroku (StepDTO).
```

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
- StepNotFoundException (HTTP 404)
  * Zwraca informację o braku kroku dla podanego ID
  
- RuntimeException (HTTP 500)
  * Ogólna obsługa błędów
  
Odpowiedź: Record ErrorResponse z timestamp, status, message, details
```

### Przepływ Danych

```
Żądanie HTTP
    ↓
RecipeController / DiscoveryController
    ↓
RecipeService
    ├─ Lokalne: RecipeRepository (JPA) → PostgreSQL
    └─ Zewnętrzne: MealDbClient → WebClient → TheMealDB API
    ↓
Odpowiedź JSON z DTO/Record
```

### Integracja TheMealDB - przykładowe

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

przykładowe wywołania przez curla:
**Wyszukiwanie po ID**
- curl https://www.themealdb.com/api/json/v1/1/lookup.php?i=52772
**Wyszukiwanie po nazwie**
- curl http://localhost:8080/api/v1/discovery/search?name=Arrabiata
**Filtrowanie po głównym składniku**
- curl http://localhost:8080/api/v1/discovery/filter/ingredient?i=chicken_breast
**Pobieranie słownika kuchni świata:**
- curl http://localhost:8080/api/v1/discovery/list?type=a


## Struktura Projektu

```
main-service/
├── src/main/java/com/cookmate/main/
│   ├── MainServiceApplication.java
│   ├── config/
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   ├── RecipeController.java      
│   │   ├── DiscoveryController.java   
│   │   └── GlobalExceptionHandler.java
│   ├── dto/                          
│   │   ├── RecipeDTO.java
│   │   ├── RecipeCreateRequest.java
│   │   ├── RecipeUpdateRequest.java
│   │   ├── RecipeListResponse.java
│   │   ├── StepDTO.java               
│   │   ├── Meal.java                  
│   │   ├── MealSearchResponse.java    
│   │   ├── CategoryResponse.java      
│   │   └── CommonListResponse.java    
│   ├── model/
│   │   ├── Recipe.java                
│   │   └── ActionType.java            
│   ├── repository/
│   │   └── RecipeRepository.java
│   └── service/
│       ├── RecipeService.java         
│       └── MealDbClient.java          
├── src/test/java/com/cookmate/main/
│   └── controller/
│       └── DiscoveryControllerTest.java 
├── src/main/resources/
│   └── application.yml
├── pom.xml                            
└── MAIN_SERVICE_README.md
```
