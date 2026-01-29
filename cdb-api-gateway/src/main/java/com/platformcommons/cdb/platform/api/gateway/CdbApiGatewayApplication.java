package com.platformcommons.cdb.platform.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ReactiveSecurityAutoConfiguration.class})
public class CdbApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdbApiGatewayApplication.class, args);
    }
}
