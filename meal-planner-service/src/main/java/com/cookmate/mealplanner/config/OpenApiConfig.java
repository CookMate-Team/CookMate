package com.cookmate.mealplanner.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CookMate Meal Planner API",
                version = "v1",
                description = "API for generating weekly meal plans and shopping lists.",
                contact = @Contact(name = "CookMate Team")
        )
)
public class OpenApiConfig {
}
