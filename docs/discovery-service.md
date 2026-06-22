# Discovery Service (Eureka)

## Przeznaczenie i Rola
`discovery-service` jest rejestrem usług (Service Registry). Eliminuje potrzebę sztywnego konfigurowania adresów IP lub portów pomiędzy komunikującymi się mikroserwisami.

## Flow
1. Po uruchomieniu, każdy mikroserwis CookMate (np. `main-service`, `gateway-service`) "zgłasza się" do `discovery-service`, przekazując swoją nazwę (np. `MAIN-SERVICE`) oraz własny adres sieciowy.
2. Serwisy co kilkadziesiąt sekund wysyłają "Heartbeat", informując, że nadal działają i są w stanie przyjmować ruch.
3. Gdy np. `simulator-service` chce pobrać dane z `main-service`, zamiast wywoływać `localhost:8081`, prosi klienta Eureka o adres serwisu pod nazwą `MAIN-SERVICE`.

## Główne Biblioteki i Zastosowanie 
- **`spring-cloud-starter-netflix-eureka-server`**: Dostarcza gotowe rozwiązanie rejestru z interfejsem graficznym. Pozwala na skalowalność (możemy odpalić np. trzy instancje `main-service`, a Eureka automatycznie wciągnie je wszystkie do puli dla load balancera).
