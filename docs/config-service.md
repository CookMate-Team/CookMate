# Config Service

## Przeznaczenie i Rola
`config-service` to serwer centralnej konfiguracji. Przechowuje zmienne środowiskowe, ustawienia bazy danych i specyficzne parametry dla każdego mikroserwisu w jednym, centralnym repozytorium (folder `config-repo/`).

## Flow
1. Config Service jako pierwsza aplikacja Springa wstaje w systemie i czyta pliki YAML z `config-repo`.
2. Gdy wstaje np. `main-service`, kontaktuje się on pod adres podany w środowisku (`CONFIG_SERVER_URL`) w pierwszej kolejności podczas fazy "bootstrap".
3. Serwis pobiera swoją konfigurację (np. namiary na bazę, port) na podstawie własnej nazwy `spring.application.name` z Config Servera i dopiero wtedy w pełni startuje.

## Główne Biblioteki i Zastosowanie (Jak to działa)
- **`spring-cloud-config-server`**: Wystawia specjalne endpointy HTTP serwujące spłaszczone konfiguracje YAML w formie plików właściwości (properties). Oddziela kod źródłowy mikrousług od infrastruktury, umożliwiając zmianę np. danych logowania do bazy w jednym pliku zamiast w pięciu osobnych projektach.
