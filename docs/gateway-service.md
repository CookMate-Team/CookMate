# Gateway Service

## Przeznaczenie i Rola
`gateway-service` to główny punkt wejścia (brama API) dla wszystkich zapytań z zewnątrz (np. z Frontendu). Odpowiada za kierowanie żądań do odpowiednich mikroserwisów na podstawie ścieżki (routing) oraz pełni rolę klienta OAuth2 w komunikacji z serwerem autoryzacji (Keycloak).

## Flow
1. Klient (np. aplikacja webowa w React/Vite) wykonuje żądanie na port `8085` (np. `/api/recipes`).
2. Gateway Service przechwytuje żądanie. Jeśli zasób wymaga autoryzacji, weryfikuje token lub przekierowuje do Keycloaka w celu zalogowania (wzorzec Token Relay).
3. Na podstawie reguł (np. `/api/recipes/**` idzie do `main-service`), Gateway komunikuje się z `discovery-service` (Eureka), aby dowiedzieć się, pod jakim IP i portem fizycznie znajduje się poszukiwany serwis.
4. Żądanie zostaje sproksowane do docelowego mikroserwisu.

## Główne Biblioteki i Zastosowanie (Jak to działa)
- **`spring-cloud-starter-gateway`**: Rdzeń serwisu. Oparty w pełni na stosie reaktywnym (WebFlux i Project Reactor), umożliwia bardzo wydajne przekierowywanie i filtrowanie zapytań bez blokowania wątków (non-blocking I/O). Idealne do asynchronicznego load-balancingu.
- **`spring-boot-starter-oauth2-client`**: Wykorzystywany do obsługi protokołu OAuth2. Gateway jest "Klientem" (Authorization Code Flow) — negocjuje wymianę tokenów i bezpiecznie przekazuje (Token Relay) uzyskany JWT do mikrousług w tle.
- **`spring-cloud-starter-netflix-eureka-client`**: Pozwala na dynamiczne lokalizowanie innych serwisów (np. `lb://MAIN-SERVICE`) zamiast twardego kodowania portów `8081`.
