package com.civiclens.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicLens API")
                        .version("0.0.1")
                        .description("REST API for authentication and civic data (representatives, watchlist, donor summaries, stances) under /api/**"));
    }
}
