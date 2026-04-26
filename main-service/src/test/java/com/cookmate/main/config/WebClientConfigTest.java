package com.cookmate.main.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(WebClientConfig.class)
class WebClientConfigTest {

    @Autowired
    private WebClient webClient;

    @Test
    void webClientBean_shouldBeCreated() {
        assertNotNull(webClient);
    }
}

