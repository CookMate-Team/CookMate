# Cooking Session Service

## Przeznaczenie i Rola
`cooking-session-service` odpowiada za interaktywną stronę gotowania "na żywo". Jego głównym zadaniem jest śledzenie aktywnych procesów w kuchni dla każdego użytkownika. Został zbudowany przy użyciu stosu reaktywnego (Spring WebFlux), co sprawia, że nadaje się idealnie do ciągłego informowania klienta frontendowego o postępach bez blokowania zasobów.

## Kluczowe Funkcjonalności
* **Zarządzanie Stanem Sesji:** Monitorowanie aktywnej sesji podczas odznaczania kolejnych wykonanych instrukcji przepisu.
* **Server-Sent Events (SSE):** Utrzymywanie trwałego połączenia z klientem, gdzie każda zmiana w krokach potrawy (`progress`) jest w czasie rzeczywistym streamowana do Frontendu.
* **Historia Gotowania:** Możliwość śledzenia dotychczasowych akcji dla konkretnych potraw (odznaczone kroki, najświeższe zapisy).
* **Bezpieczeństwo i Asynchroniczność:** Komunikacja zabezpieczona filtrami reaktywnego Spring Security (OAuth2).

## Integracje i Technologie
- Architektura: Reaktywny Spring WebFlux.
- Baza Danych: Schemat `cooking_session` wewnątrz współdzielonej bazy PostgreSQL, zapewniający separację domen.
