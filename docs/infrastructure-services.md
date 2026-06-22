# Infrastruktura Projektu (Infrastructure Services)

Ten dokument opisuje usługi, które nie zawierają samej logiki biznesowej CookMate, ale pełnią krytyczną funkcję zarządczą w architekturze mikroserwisowej.

## 1. API Gateway Service (`gateway-service`)
**Rola:** Brama dostępowa i punkt wejścia do całego ekosystemu.
**Funkcjonalności:**
- **Routing:** Wszystkie zapytania od klienta (np. zaczynające się od `/api/recipes/**` lub `/api/simulator/**`) są automatycznie przekierowywane do właściwego mikroserwisu.
- **Bezpieczeństwo (Token Relay):** Posiada konfigurację reaktywnego Spring Security (korzystając z paczek `spring-cloud-starter-gateway` i `spring-boot-starter-oauth2-resource-server`).
- **Load Balancing:** Bezpośrednia integracja z Discovery Service w celu znalezienia odpowiedniego adresu IP mikroserwisu końcowego.

## 2. Config Service (`config-service`)
**Rola:** Scentralizowane zarządzanie konfiguracją aplikacji (Spring Cloud Config Server).
**Funkcjonalności:**
- Udostępnia własności konfiguracyjne z plików umieszczonych w katalogu `config-repo/`.
- Każdy mikroserwis na starcie (bootstrapping) łączy się z tym serwisem, prosząc o swój profil konfiguracyjny (np. `main-service.yml`).
- Gwarantuje porządek w hasłach, portach (np. `spring.datasource.password`) oraz zmiennych środowiskowych, bez trzymania ich sztywno w kodzie domenowym.

## 3. Discovery Service (`discovery-service`)
**Rola:** Rejestr usług działający pod kontrolą Netflix Eureka Server.
**Funkcjonalności:**
- **Rejestracja:** Każda usługa po udanym wystartowaniu melduje się do Eureka Server (rejestruje swoją nazwę np. `MAIN-SERVICE` oraz adres IP/Port).
- **Heartbeat:** Ciągłe monitorowanie "pulsu" i stanu zdrowia podpiętych instancji.
- **Service Discovery:** Gateway (lub inne serwisy, np. Simulator dzwoniący do Main Service) prosi Eurekę o znalezienie właściwej instancji serwisu bez znajomości jego fizycznego IP w sieci.

## Inne kluczowe elementy (zewnętrzne)
- **Keycloak:** Zarządzanie tożsamością i cyklem życia tokenów JWT (Identity and Access Management).
- **PostgreSQL:** Główny silnik bazy danych podzielony logicznie na schematy (schemas) dla poszczególnych usług biznesowych, co zapobiega przenikaniu się modeli danych.
