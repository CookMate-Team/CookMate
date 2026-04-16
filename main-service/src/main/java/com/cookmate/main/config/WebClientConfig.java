package com.cookmate.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Spring WebClient and external API clients.
 */
@Configuration
public class WebClientConfig {

    /**
     * Create and configure WebClient bean for HTTP requests.
     *
     * @return configured WebClient instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .build();
    }
}
