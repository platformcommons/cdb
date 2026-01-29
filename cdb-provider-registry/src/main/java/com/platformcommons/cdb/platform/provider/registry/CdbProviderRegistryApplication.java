package com.platformcommons.cdb.platform.provider.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the CDB Provider Registry Spring Boot application.
 * <p>
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.platformcommons.cdb.*"
})
public class CdbProviderRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdbProviderRegistryApplication.class, args);
    }
}
