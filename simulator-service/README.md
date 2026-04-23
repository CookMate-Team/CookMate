# Simulator Service - Symulator Gotowania

Mikrosługa odpowiedzialna za symulowanie kroków gotowania w trybie "guided cooking". Użytkownik przegląda przepisy, a gdy wybierze "guided cooking", main-service rozbija przepis na kroki i przesyła je sekwencyjnie do simulator-service.

## Charakterystyka

- **Port**: 8082
- **Java**: 25 (Virtual Threads)
- **Spring Boot**: 4.0.5
- **Spring Cloud**: 2025.1.1
- **Komunikacja**: OpenFeign (Eureka-aware)

## Architektura

```
┌─────────────────────────────────────────┐
│    Simulator Service (:8082)            │
│  ┌───────────────────────────────────┐ │
│  │   SimulatorController             │ │
│  │ - POST /sessions/start            │ │
│  │ - POST /sessions/{id}/step        │ │
│  │ - GET  /sessions/{id}/status      │ │
│  │ - POST /sessions/{id}/pause       │ │
│  │ - POST /sessions/{id}/resume      │ │
│  │ - POST /sessions/{id}/complete    │ │
│  │ - POST /sessions/{id}/cancel      │ │
│  │ - GET  /sessions/{id}/history     │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
              ▲                  │
              │ Reports          │ Sends steps
              │ completion       │
              │                  ▼
       ┌──────────────────────────────┐
       │    Main Service              │
       │ (Recipe & Cooking Provider)  │
       └──────────────────────────────┘
```

## Funkcjonalność

### Flow Gotowania (Guided Cooking)

1. **Użytkownik w main-service**
   - Przegląda przepisy
   - Wybiera "guided cooking"

2. **Main-service przesyła kroki do simulator-service**
   - POST `/api/simulator/sessions/{sessionId}/step` z danymi kroku

3. **Simulator Service**
   - Odbiera krok z parametrami: opis, czas trwania, temperatura, waga itp.
   - Wyświetla instrukcje
   - Czeka na interakcję użytkownika
   - Odczekuje określony czas (np. 5 sekund)
   - Zwraca odpowiedź "krok ukończony"

4. **Main-service wysyła kolejny krok**
   - Proces powtarza się aż do końca przepisu

### Endpoints

| Endpoint | Metoda | Opis |
|----------|--------|------|
| `/api/simulator/sessions/start` | POST | Tworzy nową sesję gotowania |
| `/api/simulator/sessions/{sessionId}/step` | POST | Odbiera i przetwarza krok gotowania |
| `/api/simulator/sessions/{sessionId}/status` | GET | Zwraca status sesji |
| `/api/simulator/sessions/{sessionId}/pause` | POST | Pauzuje aktywną sesję |
| `/api/simulator/sessions/{sessionId}/resume` | POST | Wznawia sesję |
| `/api/simulator/sessions/{sessionId}/complete` | POST | Kończy sesję |
| `/api/simulator/sessions/{sessionId}/cancel` | POST | Anuluje sesję |
| `/api/simulator/sessions/{sessionId}/history` | GET | Zwraca historię kroków |

## Persistencja Sesji

Simulator Service zapisuje sesje i kroki do PostgreSQL:
- `simulation_sessions` - Status i postęp sesji
- `simulation_steps` - Historia kroków (`PENDING`, `EXECUTED`, `SKIPPED`)

Szczegóły: **[PERSISTENCE.md](./PERSISTENCE.md)**.

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
    password: ${DB_PASSWORD:cookmate}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8082
  tomcat:
    threads:
      max: 200

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

## Przykładowe Żądania

### Utworzenie nowej sesji

```bash
POST http://localhost:8082/api/simulator/sessions/start
```

Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "currentStep": 0,
  "totalSteps": 0,
  "message": null,
  "history": []
}
```

### Przesłanie kroku gotowania

```bash
POST http://localhost:8082/api/simulator/sessions/550e8400-e29b-41d4-a716-446655440000/step

{
  "stepNumber": 1,
  "description": "Podgrzej patelnię do temperatury 180°C",
  "durationSeconds": 5,
  "temperature": "180°C",
  "weight": "200g",
  "additionalNotes": "Używaj oliwy z oliwek extra virgin"
}
```

Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "stepNumber": 1,
  "completed": true,
  "status": "COMPLETED",
  "message": "Step completed successfully"
}
```

### Pobranie statusu sesji

```bash
GET http://localhost:8082/api/simulator/sessions/550e8400-e29b-41d4-a716-446655440000/status
```

## Technologia

- **Virtual Threads**: Lightweight threading dla wysokiej skalowlaności
- **OpenFeign**: Deklaratywne HTTP Client dla komunikacji międzyusługowej
- **Eureka**: Service Discovery
- **Spring Actuator**: Monitorowanie i metryki

## Obsługa Błędów

- **Sesja nie znaleziona**: 404 SimulationSessionNotFoundException
- **Nieprawidłowy stan**: 400 InvalidSimulationStateException
- **Komunikacja z main-service**: Logowanie i retry logic

## Pliki Dokumentacji

- **[SIMULATION.md](./SIMULATION.md)** - Szczegóły algorytmu symulacji
- **[PERSISTENCE.md](./PERSISTENCE.md)** - Strategia przechowywania stanu
