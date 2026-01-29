package com.platformcommons.cdb.platform.master.data.config;

import com.platformcommons.cdb.common.openapi.BaseOpenApiConfig;
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

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CDB Master Data Engine",
                version = "v1.0",
                description = "Master Data management, taxonomy, and synchronization APIs",
                contact = @Contact(name = "CDB Platform Team", email = "platform@cdb.gov"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
        ),
        servers = {
                @Server(url = "https://api.cdb.gov/master-data", description = "Production"),
                @Server(url = "https://staging-api.cdb.gov/master-data", description = "Staging"),
                @Server(url = "http://localhost:8084", description = "Development")
        },
        security = {@SecurityRequirement(name = "bearerAuth")}
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig extends BaseOpenApiConfig {

    @Bean
    public GroupedOpenApi masterDataApi() {
        return groupedApi("cdb-master-data-engine", "/api/v1/**");
    }
}
