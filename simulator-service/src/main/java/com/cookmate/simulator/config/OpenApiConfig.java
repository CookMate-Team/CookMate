package com.cookmate.simulator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CookMate Simulator API",
                version = "v1",
                description = "API for one-click cooking step simulation with session recovery.",
                contact = @Contact(name = "CookMate Team")
        ),
        servers = {
                @Server(url = "http://localhost:8082", description = "Local")
        }
)
public class OpenApiConfig {
}
