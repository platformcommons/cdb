package com.platformcommons.cdb.auth.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CDB Auth Registry Application
 * 
 * Main application class for the CDB Authentication and Authorization Registry service.
 * Provides user management, application registration, JWT/OAuth2 authentication,
 * and authorization services for the CDB ecosystem.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 */
@SpringBootApplication
public class CdbAuthRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdbAuthRegistryApplication.class, args);
    }
}