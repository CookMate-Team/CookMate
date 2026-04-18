# Simulator Service - Symulator Planowania Posiłków

Mikrousługa odpowiedzialna za generowanie planów posiłków na podstawie przepisów dostępnych w main-service. Wykorzystuje architekturę mikroserwisową z Eureka i OpenFeign do komunikacji.

## Charakterystyka

- **Port**: 8082
- **Java**: 25 (Virtual Threads Preview)
- **Spring Boot**: 4.0.5
- **Spring Cloud**: 2025.1.1
- **Komunikacja**: OpenFeign (Eureka-aware)

## Architektura

```
┌─────────────────────────────────────┐
│    Simulator Service (:8082)        │
│  ┌─────────────────────────────┐   │
│  │   SimulatorController       │   │
│  │ - /api/simulator/recipes    │   │
│  │ - /api/simulator/meal-plan  │   │
│  │ - /api/simulator/health-..  │   │
│  └────────────┬────────────────┘   │
└───────────────┼───────────────────┘
                │ OpenFeign
                ▼
       ┌─────────────────────┐
       │   Main Service      │
       │ (Recipe Provider)   │
       └─────────────────────┘
```

## Funkcjonalność

### Endpoints

| Endpoint | Metoda | Parametry | Opis |
|----------|--------|-----------|------|
| `/api/simulator/recipes` | GET | - | Pobiera listę wszystkich przepisów z main-service |
| `/api/simulator/recipes/{id}` | GET | `id` (Long) | Pobiera szczegóły konkretnego przepisu |
| `/api/simulator/meal-plan` | GET | `days` (int, default=3) | Generuje losowy plan posiłków na X dni |
| `/api/simulator/health-check` | GET | - | Sprawdza dostępność main-service i zwraca status |
| `/api/simulator/sessions/start` | POST | `days` (int, optional) | Tworzy nową sesję symulacji i zapisuje kroki w bazie |
| `/api/simulator/sessions/{sessionId}/status` | GET | `sessionId` | Zwraca status sesji (recovery po F5) |
| `/api/simulator/sessions/{sessionId}/steps/execute` | POST | `sessionId` | Wykonuje kolejny krok symulacji |
| `/api/simulator/sessions/{sessionId}/pause` | POST | `sessionId` | Pauzuje aktywną sesję |
| `/api/simulator/sessions/{sessionId}/resume` | POST | `sessionId` | Wznawia zapauzowaną sesję |
| `/api/simulator/sessions/{sessionId}/complete` | POST | `sessionId` | Kończy sesję symulacji |
| `/api/simulator/sessions/{sessionId}/cancel` | POST | `sessionId` | Anuluje sesję symulacji |
| `/api/simulator/sessions/{sessionId}/history` | GET | `sessionId` | Zwraca historię kroków sesji |
| `/actuator/health` | GET | - | Health check serwisu |

### Przykładowe Odpowiedzi

**GET /api/simulator/meal-plan?days=5**

```json
{
  "days": 5,
  "totalRecipes": 42,
  "plan": [
    {
      "day": 1,
      "recipeId": 15,
      "recipeName": "Spaghetti Carbonara",
      "preparationTime": "30 minutes"
    },
    {
      "day": 2,
      "recipeId": 23,
      "recipeName": "Chicken Stir Fry",
      "preparationTime": "25 minutes"
    }
  ]
}
```

**GET /api/simulator/health-check**

```json
{
  "status": "OK",
  "mainService": "REACHABLE",
  "recipeCount": "42"
}
```

## Konfiguracja

### Application.yml

```yaml
spring:
  application:
    name: simulator-service
  threads:
    virtual:
      enabled: true        # Włączenie Virtual Threads (Java 25)
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:cookmate}
    username: ${DB_USER:cookmate}

server:
  port: 8082
  tomcat:
    threads:
      max: 200            # Max thread pool

feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: basic
  compression:
    request:
      enabled: true
    response:
      enabled: true

eureka:
  client:
    service-url:
      defaultZone: http://discovery-service:8761/eureka/
```

## Uruchomienie

### Docker Compose (zalecane)

```bash
# Z głównego katalogu projektu
docker compose up --build simulator-service
```

### Lokalne uruchomienie

```bash
cd simulator-service
mvn spring-boot:run
```

Serwis będzie dostępny pod: `http://localhost:8082`

## Integracja z Main Service

Simulator Service komunikuje się z Main Service za pośrednictwem OpenFeign:

- **Client**: `MainServiceClient` (src/main/java/com/cookmate/simulator/client/)
- **Base URL**: Pobierana z Eureka Registry
- **Endpoints**: `/api/recipes`, `/api/recipes/{id}`

## Persistencja Sesji

Simulator Service zapisuje sesje i kroki do PostgreSQL:
- `simulation_sessions` - status i postęp sesji,
- `simulation_steps` - historia kroków (`PENDING`, `EXECUTED`, `SKIPPED`).

Szczegóły: **[PERSISTENCE.md](./PERSISTENCE.md)**.

## Obsługa Błędów

Serwis obsługuje scenariusze awaryjne:

- **Main Service Niedostępny**: Endpoint `/api/simulator/health-check` zwraca status "DEGRADED"
- **Brak Przepisów**: Endpoint `/api/simulator/meal-plan` zwraca wiadomość: "No recipes available"

## Technologia

- **Virtual Threads**: Lightweight threading dla wysokiej skalowlaności
- **OpenFeign**: Deklaratywne HTTP Client dla komunikacji międzyusługowej
- **Eureka**: Service Discovery
- **Spring Actuator**: Monitorowanie i metryki

## Pliki Dokumentacji

- **[SIMULATION.md](./SIMULATION.md)** - Szczegóły algorytmu symulacji
- **[PERSISTENCE.md](./PERSISTENCE.md)** - Strategia przechowywania stanu
