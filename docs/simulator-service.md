# Simulator Service

## Przeznaczenie i Rola
`simulator-service` został stworzony do demonstracji i weryfikacji logiki procesów kulinarnych w systemie (odseparowanej od pełnej implementacji SSE w Cooking Session Service). Udostępnia "one-click" podejście do uruchomienia symulacji gotowania. Pozwala na testowanie przebiegu, cofanie się ("Rewind") oraz szybkie asynchroniczne symulowanie wykonania pełnego przepisu.

## Kluczowe Funkcjonalności
* **Uruchamianie Symulacji:** Tworzenie sesji symulacyjnych. Mikroserwis z pomocą OpenFeign komunikuje się z `main-service` celem pobrania ustandaryzowanych kroków przygotowania z potraw ustrukturyzowanych przez sztuczną inteligencję.
* **Egzekucja kroków:** Udostępnia endpointy do ręcznego wysyłania komendy `executeNextStep`, umożliwiającej przechodzenie przez kolejne etapy przepisu.
* **Cofanie Procesu (Rewind):** Unikatowa funkcjonalność pozwalająca na "przewinięcie" symulacji wstecz do podanego indeksu.
* **Historia Sesji:** W przeciwieństwie do strumieniowania, serwuje całą gotową historię stanu symulacji.

## Integracje i Technologie
- Komunikacja Międzysystemowa: Integracja z `main-service` odbywa się przez interfejs `FeignClient`.
- Pętla Zdarzeń: Posiada mechanizmy asynchronicznego przekazywania logów na temat ukończonych operacji (wykorzystywane przy monitorowaniu "na żywo" w infrastrukturze demonstracyjnej).

## Flow Symulacji i Tabele
1. **Zainicjowanie:** Kiedy użytkownik wysyła request (`POST /sessions/start`), symulator prosi `main-service` o listę kroków. Zostają one zachowane w tabeli `simulation_steps` ze statusem `PENDING`. Dodawany jest również główny wpis sesji do tabeli `simulation_sessions`.
2. **Kolejne kroki:** Przy wywoływaniu akcji (`POST /sessions/{sessionId}/steps/execute`) symulator wczytuje najstarszy wpis `PENDING`, zmienia jego stan na `EXECUTED` i wysyła asynchroniczne powiadomienie (notyfikację progresu) do `cooking-session-service`.
3. **Zakończenie i Przewijanie:** Gdy wszystkie kroki otrzymają status `EXECUTED`, sesja zostaje zamknięta (`COMPLETED`). Ponadto zaimplementowano funkcjonalność **Rewind** (`POST /rewind?stepNumber=N`), która umożliwia cofnięcie sesji do wskazanego kroku bez jej utraty czy wymogu resetowania wszystkich danych.

## Główne Biblioteki i Zastosowanie 
- **`spring-boot-starter-web`**: Podstawa dla `SimulatorController`, używana do odbierania poleceń od klienta (np. akcje symulacji `POST`). W przeciwieństwie do `cooking-session-service`, tutaj użyto klasycznego modelu MVC bez asynchronicznych strumieni.
- **`spring-cloud-starter-openfeign`**: Oparta na interfejsach deklaratywna implementacja klienta REST. Użyta w `MainServiceClient`, pozwala symulatorowi na pobieranie szczegółów przepisu i kroków bezpośrednio z głównego serwisu w sposób bardzo podobny do wywoływania lokalnych metod Javy, znacznie upraszczając kod i integrację (Feign pod spodem rozmawia z usługą Discovery, aby znaleźć docelowy adres `MAIN-SERVICE`).
- **`spring-boot-starter-data-jpa`**: Mapowanie obiektowo-relacyjne dla PostgreSQL (tabele z logami `simulation_steps` oraz `simulation_sessions`). Pozwala na proste zapisywanie, odczytywanie i modyfikowanie (metoda Rewind) logów i postępu na wybranym przepisie, aby trwale śledzić cykl życia symulacji.
