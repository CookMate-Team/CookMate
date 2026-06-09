# Konfiguracja mikroserwisów jako Spring Security Resource Servers

## Opis problemu

Mikroserwisy biznesowe (main-service, cooking-session-service, simulator-service) za API Gateway muszą działać jako OAuth2 Resource Servers — przyjmować tokeny JWT z nagłówka `Authorization`, weryfikować je kryptograficznie wobec Keycloak i egzekwować autoryzację na bazie ról (Claims).

Aktualnie:
- **Gateway** jest skonfigurowany jako OAuth2 Client (`oauth2Login` + `TokenRelay`) — to jest poprawne i nie zmieni się
- **Mikroserwisy** nie mają żadnej konfiguracji bezpieczeństwa — są całkowicie otwarte
- Keycloak definiuje role realm: `ROLE_USER`, `ROLE_ADMIN`
- Role w JWT Keycloak znajdują się w claim `realm_access.roles`

## Proponowane zmiany

### 1. Scentralizowana konfiguracja JWT (config-repo)

#### [MODIFY] [application.yml](file:///d:/repos/sumatywny/config-repo/application.yml)

Dodanie wspólnej konfiguracji Resource Server do globalnego pliku `application.yml`, który jest ładowany przez wszystkie serwisy przez Config Server. Dzięki temu konfiguracja JWT jest w jednym miejscu.

```yaml
# Dodane na końcu istniejącej konfiguracji:
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_INTERNAL_URL:http://keycloak:8080}/realms/cookmate
          jwk-set-uri: ${KEYCLOAK_INTERNAL_URL:http://keycloak:8080}/realms/cookmate/protocol/openid-connect/certs
```

> [!NOTE]
> Gateway nadpisuje tę konfigurację swoim OAuth2 Client — nie koliduje to z Resource Server w pozostałych serwisach.

---

### 2. Zależności Maven

#### [MODIFY] [pom.xml](file:///d:/repos/sumatywny/main-service/pom.xml) (main-service)
#### [MODIFY] [pom.xml](file:///d:/repos/sumatywny/cooking-session-service/pom.xml) (cooking-session-service)
#### [MODIFY] [pom.xml](file:///d:/repos/sumatywny/simulator-service/pom.xml) (simulator-service)

Dodanie zależności do każdego serwisu biznesowego:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

### 3. Wspólna klasa konwertera ról Keycloak

Keycloak umieszcza role realm w niestandardowym claimie `realm_access.roles`. Potrzebujemy `JwtAuthenticationConverter`, który je parsuje i tłumaczy na `GrantedAuthority`.

> [!IMPORTANT]
> Ponieważ projekt nie ma modułu `common` (każdy serwis ma osobny `spring-boot-starter-parent`), klasa `KeycloakJwtAuthenticationConverter` zostanie utworzona w każdym serwisie. Jednakże, żeby uniknąć powielania logiki, klasa jest identyczna we wszystkich serwisach — różni się jedynie pakietem.

**Alternatywa**: Stworzyć moduł `common` Maven jako bibliotekę współdzieloną. Ale to wykracza poza scope tego issue i wymaga zmian w architekturze buildu.

#### [NEW] `KeycloakJwtAuthenticationConverter.java` — w pakiecie `config` każdego serwisu

```java
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.replace("ROLE_", "")))
                .collect(Collectors.toList());
    }
}
```

> [!NOTE]
> Role Keycloak są zdefiniowane jako `ROLE_USER`, `ROLE_ADMIN`. Konwerter normalizuje je — zapewnia prefiks `ROLE_` (bez powielania) tak aby `hasRole('ROLE_USER')` działało poprawnie w Spring Security.

---

### 4. SecurityFilterChain — konfiguracja per serwis

#### [NEW] `SecurityConfig.java` — main-service (`com.cookmate.main.config`)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            KeycloakJwtAuthenticationConverter converter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, accessDeniedException) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                })
            )
            .build();
    }
}
```

#### [NEW] `SecurityConfig.java` — cooking-session-service (`com.cookmate.cookingsession.config`)

Analogiczna konfiguracja, ale cooking-session-service używa WebFlux (ma `spring-boot-starter-webflux`), więc musi to być **reactive** security:

```java
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverterAdapter converter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
            )
            .build();
    }
}
```

#### [NEW] `SecurityConfig.java` — simulator-service (`com.cookmate.simulator.config`)

Analogiczna do main-service (obie to serwisy MVC/Servlet).

---

### 5. Zabezpieczenie endpointów adnotacjami `@PreAuthorize`

#### [MODIFY] [RecipeController.java](file:///d:/repos/sumatywny/main-service/src/main/java/com/cookmate/main/controller/RecipeController.java)

| Metoda | Reguła |
|--------|--------|
| `GET /api/recipes` | `authenticated` (przez SecurityFilterChain) |
| `GET /api/recipes/{id}` | `authenticated` |
| `POST /api/recipes` | `@PreAuthorize("hasRole('ROLE_USER')")` |
| `PUT /api/recipes/{id}` | `@PreAuthorize("hasRole('ROLE_USER')")` |
| `DELETE /api/recipes/{id}` | `@PreAuthorize("hasRole('ROLE_ADMIN')")` |

#### [MODIFY] [StepController.java](file:///d:/repos/sumatywny/main-service/src/main/java/com/cookmate/main/controller/StepController.java)

| Metoda | Reguła |
|--------|--------|
| `GET /api/steps/{stepId}` | `authenticated` |
| `POST /api/steps/generate` | `@PreAuthorize("hasRole('ROLE_USER')")` |

#### [MODIFY] [SimulatorController.java](file:///d:/repos/sumatywny/simulator-service/src/main/java/com/cookmate/simulator/controller/SimulatorController.java)

| Metoda | Reguła |
|--------|--------|
| `POST /api/simulator/sessions/start` | `@PreAuthorize("hasRole('ROLE_USER')")` |
| Pozostałe endpointy | `authenticated` (przez SecurityFilterChain) |

#### [MODIFY] [CookingSessionController.java](file:///d:/repos/sumatywny/cooking-session-service/src/main/java/com/cookmate/cookingsession/controller/CookingSessionController.java)

| Metoda | Reguła |
|--------|--------|
| `POST /api/cooking-sessions/progress` | `@PreAuthorize("hasRole('ROLE_USER')")` |
| Pozostałe endpointy | `authenticated` (przez SecurityFilterChain) |

---

### 6. Aktualizacja konfiguracji testowej

#### [MODIFY] [application-test.yml](file:///d:/repos/sumatywny/main-service/src/test/resources/application-test.yml) (main-service)

Dodanie wyłączenia Resource Server w testach (testy nie mają dostępu do Keycloak):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9999/realms/test
```

Plus dodanie `spring-security-test` dependency i ewentualne użycie `@WithMockUser` lub `@MockBean JwtDecoder` w testach.

#### [MODIFY] [pom.xml](file:///d:/repos/sumatywny/main-service/pom.xml) — dodanie `spring-security-test`

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Analogicznie dla cooking-session-service i simulator-service.

---

### 7. Gateway — dodanie resource-server do weryfikacji JWT

#### [MODIFY] [pom.xml](file:///d:/repos/sumatywny/gateway-service/pom.xml)

Dodanie `spring-boot-starter-oauth2-resource-server` aby gateway mógł weryfikować JWT i egzekwować role na poziomie routingu.

#### [MODIFY] [SecurityConfig.java](file:///d:/repos/sumatywny/gateway-service/src/main/java/com/cookmate/gateway/config/SecurityConfig.java)

Dodanie `.oauth2ResourceServer()` do istniejącej konfiguracji, co pozwoli na obsługę zarówno sesji (OAuth2 Login) jak i Bearer tokenów:

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(Customizer.withDefaults())
)
```

#### [MODIFY] [gateway-service.yml](file:///d:/repos/sumatywny/config-repo/gateway-service.yml)

Dodanie konfiguracji Resource Server JWT:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_INTERNAL_URL:http://keycloak:8080}/realms/cookmate
          jwk-set-uri: ${KEYCLOAK_INTERNAL_URL:http://keycloak:8080}/realms/cookmate/protocol/openid-connect/certs
```

---

## Open Questions

> [!IMPORTANT]
> **Moduł common vs duplikacja**: Konwerter ról Keycloak (`KeycloakJwtAuthenticationConverter`) będzie zduplikowany w 3 serwisach (main, cooking-session, simulator). Czy wolisz żebym stworzył moduł `common` Maven, czy akceptujesz duplikację w ramach tego issue?

> [!IMPORTANT]
> **Reguły autoryzacji endpointów**: Zaproponowałem rozsądne reguły (GET = authenticated, POST/PUT = ROLE_USER, DELETE = ROLE_ADMIN). Czy te reguły są właściwe dla Twojego projektu, czy masz inne wymagania?

> [!IMPORTANT]
> **DiscoveryController (`/api/v1/discovery/*`)**: Ten kontroler pobiera dane z TheMealDB. Czy powinien być chroniony (authenticated), czy publicznie dostępny?

## Plan weryfikacji

### Testy automatyczne
- Istniejące testy kontrolerów zostaną zaktualizowane o `@WithMockUser` / `@WithMockJwtUser` oraz `spring-security-test`
- Testy SecurityConfig dla gateway (istniejące) zostaną zaktualizowane

### Weryfikacja manualna
- Po uruchomieniu Docker Compose:
  - Żądanie bez tokenu → **401 Unauthorized**
  - Żądanie z nieprawidłowym tokenem → **401 Unauthorized**
  - Żądanie z poprawnym tokenem bez wymaganej roli → **403 Forbidden**
  - Żądanie z poprawnym tokenem i rolą → **200 OK**
