# Keycloak

## Przeznaczenie i Rola
Keycloak odpowiada w architekturze CookMate za scentralizowane zarządzanie tożsamością i dostępem (Identity and Access Management - IAM). Świadczy usługi autoryzacji oparte o protokoły OpenID Connect (OIDC) i OAuth 2.0. Zdejmuje on ciężar implementacji logowania z każdego poszczególnego mikroserwisu.

## Flow
1. Użytkownik CookMate próbuje uzyskać dostęp do zabezpieczonych zasobów.
2. Gateway Service wykrywa brak aktywnej sesji i przekierowuje użytkownika na gotowy, wystawiony przez Keycloaka ekran logowania.
3. Po poprawnym zalogowaniu (lub rejestracji), Keycloak generuje cyfrowo podpisany token JWT i przekazuje go do bramy.
4. Mikroserwisy zabezpieczone Spring Security (np. `main-service`) przyjmują ten token z nagłówka `Authorization: Bearer` i na podstawie jego kryptograficznego podpisu samodzielnie weryfikują (Resource Server) czy użytkownik ma uprawnienia (np. `ROLE_USER`) do wykonania danej operacji.

## Główne Technologie i Zastosowanie (Jak to działa)
- **Keycloak (Obraz Dockerowy: `quay.io/keycloak/keycloak`)**: Gotowy serwer tożsamości stworzony przez Red Hat. Skonfigurowany przez importowanie Realm (`realm-export.json`), co pozwala od razu na korzystanie ze wstępnie ustawionych ról, użytkowników (np. konta testowego) i polityk bezpieczeństwa na starcie środowiska Docker Compose bez konieczności robienia tego ręcznie.
- Posiada własną odizolowaną bazę na konfigurację autoryzacji (zapisywaną w PostgreSQL w osobnym schemacie/bazie).
