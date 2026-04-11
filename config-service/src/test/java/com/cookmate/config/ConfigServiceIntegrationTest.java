package com.cookmate.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.server.native.search-locations=file:../config-repo"
})
class ConfigServiceIntegrationTest {

    record ConfigResponse(String name, List<PropertySource> propertySources) {}
    record PropertySource(String name, Map<String, Object> source) {}

    @Autowired
    private RestTestClient client;

    @Test
    void shouldLoadApplicationYaml() {
        client.get()
                .uri("/application/default")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConfigResponse.class).value(response -> {
                    assertThat(response.propertySources())
                            .isNotEmpty();

                    var globalSource = response.propertySources().stream()
                            .filter(ps -> ps.name().contains("application.yml"))
                            .findFirst()
                            .map(PropertySource::source)
                            .orElseThrow();

                    assertThat(globalSource.get("management.endpoints.web.exposure.include")).isEqualTo("health,info,metrics");
                });
    }

    @Test
    void shouldLoadMainServiceYaml() {
        client.get()
                .uri("/main-service/default")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConfigResponse.class).value(response -> {
                    assertThat(response.name()).isEqualTo("main-service");
                    assertThat(response.propertySources()).isNotEmpty();

                    var serviceSource = response.propertySources().stream()
                            .filter(ps -> ps.name().contains("main-service.yml"))
                            .findFirst()
                            .map(PropertySource::source)
                            .orElseThrow(() -> new AssertionError("Nie znaleziono pliku main-service.yml w odpowiedzi"));

                    assertThat(serviceSource.get("server.port")).isEqualTo(8081);
                });
    }
}