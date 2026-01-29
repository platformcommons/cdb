package com.platformcommons.cdb.platform.api.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.platformcommons.cdb.*")
public class CdbApiRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdbApiRegistryApplication.class, args);
    }
}
