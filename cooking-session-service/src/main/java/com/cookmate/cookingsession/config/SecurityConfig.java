package com.cookmate.cookingsession.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;

/**
 * Reactive security configuration for cooking-session-service acting as an OAuth2 Resource Server.
 * <p>
 * This service uses WebFlux, so the security configuration is reactive.
 * All API endpoints require authentication via a valid JWT token.
 * Fine-grained role-based access is enforced at the method level using {@code @PreAuthorize}.
 * Actuator health and info endpoints are publicly accessible for monitoring.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         KeycloakJwtAuthenticationConverter jwtAuthenticationConverter) {
        ReactiveJwtAuthenticationConverterAdapter reactiveConverter =
                new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);

        ServerBearerTokenAuthenticationConverter bearerTokenConverter = new ServerBearerTokenAuthenticationConverter();
        bearerTokenConverter.setAllowUriQueryParameter(true);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(bearerTokenConverter)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveConverter))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )
                .build();
    }
}
