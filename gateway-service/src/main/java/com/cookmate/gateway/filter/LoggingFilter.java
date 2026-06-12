package com.cookmate.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpHeaders headers = exchange.getRequest().getHeaders();
        logger.info("Gateway received request for path: {}", path);
        logger.info("Request headers: Cookie={}, Authorization={}", 
            headers.get("Cookie") != null, headers.get("Authorization") != null);
        
        return exchange.getSession().flatMap(session -> {
            logger.info("Session ID: {}, isStarted: {}", session.getId(), session.isStarted());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                logger.info("Response for {}: Status {}", path, exchange.getResponse().getStatusCode());
            }));
        });
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
