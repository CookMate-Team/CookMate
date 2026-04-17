# Persistence - Przechowywanie Stanu i Caching

## Strategia Przechowywania Stanu

### Charakterystyka Simulator Service

Simulator Service jest **stateless** - nie przechowuje stanu pomiędzy żądaniami. Każde żądanie jest niezależne i oparte na aktualnym stanie main-service.

```
┌──────────────┐
│  Request 1   │──→ Fetch recipes from main-service → Generate plan
└──────────────┘

┌──────────────┐
│  Request 2   │──→ Fetch recipes from main-service → Generate plan (different)
└──────────────┘

(Brak współdzielonego stanu między requestami)
```

## Caching

### Brak Cache'u na Poziomie Hosta

Simulator Service **nie implementuje cache'u** lokalnego:
- Każdy request pobiera świeżą listę przepisów
- Gwarantuje zawsze aktualne plany posiłków
- Brak ryzyka stale'ego cache'u

### Cache'owanie na Poziomie Sieci (Opcjonalne)

Można skonfigurować cache'owanie HTTP w API Gateway:

```yaml
# Przykład cache-control headers
Cache-Control: max-age=60, public
```

## Dane Tymczasowe

### Memory Usage

Każde żądanie wykorzystuje:
- ~100KB dla listy przepisów (avg)
- ~50KB dla struktury planu posiłków
- **Total: ~150KB per request**

Virtual Threads pozwalają na efektywne zarządzanie pamięcią dzięki lekkiemu footprintowi.

### Garbage Collection

- GC odbywa się automatycznie po zaplanowaniu żądania
- Typ: ZGC (z25) lub G1GC (domyślny dla Java 25)
- Stop-the-world: < 1ms (przeciętnie)

## Baza Danych

### Brak Bazy Danych w Simulator Service

Simulator Service **nie ma własnej bazy danych**:
- Wszystkie dane pochodzą z main-service
- Nie przechowuje recepty, użytkowników, ani historii planów
- Czysto **query-only** service

Architektura:

```
┌──────────────────────────┐
│ Simulator Service        │
│  (Stateless, no DB)      │
└────────────┬─────────────┘
             │
             ▼ (OpenFeign)
┌──────────────────────────┐
│ Main Service             │
│ (PostgreSQL, contains    │
│  all recipes and data)   │
└──────────────────────────┘
```

## Skalowanie Horyzontalnego

Simulator Service można skalować horyzontalnie bez problemów:

```
┌────────────────┐
│ Load Balancer  │
└────────┬───────┘
         │
         ├──→ Simulator Instance 1 (:8082)
         ├──→ Simulator Instance 2 (:8082)
         ├──→ Simulator Instance 3 (:8082)
         └──→ Simulator Instance N (:8082)
                    ↓
              Main Service (single)
```

Każda instancja:
- Jest całkowicie stateless
- Może być uruchamiana/zatrzymywana bez wpływu na inne
- Komunikuje się z tym samym main-service
- Automatycznie rejestruje się w Eureka

## Życiorys Żądania

### Żądanie: GET /api/simulator/meal-plan?days=5

```
1. [T=0ms]    Virtual Thread zostaje stworzony
2. [T=10ms]   SimulatorController#generateMealPlan() się uruchamia
3. [T=20ms]   MainServiceClient.getAllRecipes() - HTTP call via OpenFeign
4. [T=150ms]  Odpowiedź z main-service (42 recepty, ~100KB)
5. [T=160ms]  Random selection algo (5 iteracji)
6. [T=170ms]  Serializacja do JSON (~2KB)
7. [T=180ms]  Response wysyłana do klienta
8. [T=185ms]  Virtual Thread wznawia pool
9. [T=200ms]  Garbage Collection (automatycznie)
```

**Total Latency**: ~185ms (network dominant)
**Memory Retained**: 0B (wszystko zwolnione)

## Event Sourcing (Opcjonalne)

Dla przyszłych wymagań audit/analytics:

```
Opcjonalna architektura:
- Każdy plan posiłków mógłby być logowany do event stream'u
- Przechowywanie: Message Queue (RabbitMQ) lub Event Log (PostgreSQL)
- Cel: Analiza popularności przepisów, trendów itp.

Nie jest zaimplementowane w bieżącej wersji.
```

## Podsumowanie

| Aspekt | Implementacja |
|--------|---------------|
| **Stan Aplikacji** | Stateless |
| **Cache Lokalny** | Brak |
| **Baza Danych** | Nie posiada |
| **Pamięć per Request** | ~150KB |
| **Skalowanie** | Horyzontal (stateless) |
| **Durability** | N/A (nie przechowuje) |
| **GC Strategy** | Automatyczne (Virtual Thread friendly) |

Simulator Service jest zaprojektowany dla:
- ✅ Wysokiej dostępności
- ✅ Skalowalności horyzontalnej
- ✅ Niskiego latency
- ✅ Minimalnego zużycia zasobów
