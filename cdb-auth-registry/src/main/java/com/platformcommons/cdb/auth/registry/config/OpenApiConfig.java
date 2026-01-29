package com.platformcommons.cdb.auth.registry.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration
 * 
 * Configuration class for OpenAPI/Swagger documentation setup.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 * @since 2024
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CDB Auth Registry",
                version = "v1.0",
                description = "Authentication and Authorization services for CDB",
                contact = @Contact(name = "CDB Platform Team", email = "platform@cdb.gov"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
        ),
        servers = {
                @Server(url = "https://api.cdb.gov/auth-registry", description = "Production"),
                @Server(url = "https://staging-api.cdb.gov/auth-registry", description = "Staging"),
                @Server(url = "http://localhost:8083", description = "Development")
        },
        security = {@SecurityRequirement(name = "bearerAuth")}
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("cdb-auth-registry-public")
                .pathsToMatch("/api/v1/public/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("cdb-auth-registry-admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}