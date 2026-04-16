# CookMate

Mikrousługowa architektura aplikacji CookMate do zarządzania przepisami kulinarnymi.

## Architektura

```
                    ┌─────────────────┐
                    │  config-service │  :8888
                    │ @EnableConfigServer│
                    └────────┬────────┘
                             │ reads
                    ┌────────▼────────┐
                    │   config-repo/  │  (pliki YAML)
                    └─────────────────┘
                             │ serves config to
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
   ┌─────────────────┐ ┌───────────┐ ┌──────────────────┐
   │discovery-service│ │main-service│ │simulator-service │
   │@EnableEurekaServer│:8081      │ │     :8082        │
   │     :8761       │ │@EnableDisc│ │@EnableDiscovery  │
   └────────┬────────┘ └─────┬─────┘ └────────┬─────────┘
            │  registers     │  registers      │
            └────────────────┴─────────────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │  :5432
                    └─────────────────┘
```

## Serwisy i porty

| Serwis              | Port | Opis                                      |
|---------------------|------|-------------------------------------------|
| `config-service`    | 8888 | Spring Cloud Config Server                |
| `discovery-service` | 8761 | Eureka Discovery Server                   |
| `main-service`      | 8081 | REST API zarządzania przepisami + PostgreSQL|
| `simulator-service` | 8082 | Symulator planowania posiłków (Feign)     |
| `postgres`          | 5432 | Baza danych PostgreSQL                    |

## Stos technologiczny

- **Java 25** / **Spring Boot 4.0.5**
- **Spring Cloud 2024.0.0** (Config, Eureka, OpenFeign, LoadBalancer)
- **Spring Data JPA** + **PostgreSQL**
- **Docker** (multi-stage build) + **Docker Compose**

## Uruchomienie

### Docker Compose (zalecane)

```bash
# Zbuduj i uruchom wszystkie serwisy
docker compose up --build

# Uruchom w tle
docker compose up --build -d

# Zatrzymaj
docker compose down
```

Kolejność startu: **PostgreSQL → Config → Discovery → main-service / simulator-service**

### Lokalne uruchomienie (każdy serwis osobno)

```bash
# 1. Uruchom PostgreSQL
docker run -e POSTGRES_DB=cookmate -e POSTGRES_USER=cookmate \
           -e POSTGRES_PASSWORD=cookmate -p 5432:5432 postgres:16-alpine

# 2. config-service
cd config-service && mvn spring-boot:run

# 3. discovery-service
cd discovery-service && mvn spring-boot:run

# 4. main-service
cd main-service && mvn spring-boot:run

# 5. simulator-service
cd simulator-service && mvn spring-boot:run
```

## Endpointy

### main-service (`http://localhost:8081`)

| Metoda | Ścieżka               | Opis                     |
|--------|-----------------------|--------------------------|
| GET    | `/api/recipes`        | Lista wszystkich przepisów|
| GET    | `/api/recipes?name=X` | Szukaj przepisu po nazwie|
| GET    | `/api/recipes/{id}`   | Pobierz przepis           |
| POST   | `/api/recipes`        | Utwórz przepis            |
| PUT    | `/api/recipes/{id}`   | Zaktualizuj przepis       |
| DELETE | `/api/recipes/{id}`   | Usuń przepis              |
| GET    | `/actuator/health`    | Health check              |

### simulator-service (`http://localhost:8082`)

| Metoda | Ścieżka                         | Opis                              |
|--------|---------------------------------|-----------------------------------|
| GET    | `/api/simulator/recipes`        | Lista przepisów (via main-service)|
| GET    | `/api/simulator/recipes/{id}`   | Szczegóły przepisu                |
| GET    | `/api/simulator/meal-plan?days=7`| Wygeneruj plan posiłków           |
| GET    | `/api/simulator/health-check`   | Sprawdź połączenie z main-service |
| GET    | `/actuator/health`              | Health check                      |

### discovery-service (`http://localhost:8761`)

Eureka Dashboard dostępny pod: `http://localhost:8761`

### config-service (`http://localhost:8888`)

```
GET http://localhost:8888/main-service/default
GET http://localhost:8888/simulator-service/default
GET http://localhost:8888/application/default
```

## Struktura projektu

```
CookMate/
├── config-repo/                    # Pliki konfiguracyjne serwowane przez Config Server
│   ├── application.yml             # Globalna konfiguracja (wszystkie serwisy)
│   ├── main-service.yml            # Konfiguracja main-service
│   └── simulator-service.yml       # Konfiguracja simulator-service
├── config-service/                 # Spring Cloud Config Server (:8888)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/cookmate/config/ConfigServiceApplication.java
├── discovery-service/              # Eureka Discovery Server (:8761)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/cookmate/discovery/DiscoveryServiceApplication.java
├── main-service/                   # Serwis przepisów (:8081)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/cookmate/main/
│       ├── MainServiceApplication.java
│       ├── controller/RecipeController.java
│       ├── model/Recipe.java
│       ├── repository/RecipeRepository.java
│       └── service/RecipeService.java
├── simulator-service/              # Serwis symulatora (:8082)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/cookmate/simulator/
│       ├── SimulatorServiceApplication.java
│       ├── client/MainServiceClient.java
│       ├── controller/SimulatorController.java
│       └── dto/RecipeDto.java
├── docker-compose.yml
└── pom.xml                         # Root Maven aggregator
```
[MAIN_SERVICE_README.md](./MAIN_SERVICE_README.md).
