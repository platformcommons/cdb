package com.platformcommons.cdb.security.config;

import com.platformcommons.cdb.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtTokenService.class)
    public JwtTokenService jwtTokenService(
            @Value("${cdb.security.jwt.rsa.private-key:}") String rsaPrivateKeyPem,
            @Value("${cdb.security.jwt.rsa.public-key:}") String rsaPublicKeyPem,
            @Value("${cdb.security.jwt.kid:}") String keyId
    ) {
        String priv = rsaPrivateKeyPem;
        String pub = rsaPublicKeyPem;
        if (isBlank(pub)) {
            pub = readClasspath("/cdb-jwt-keys/public.pem");
        }
        if (isBlank(priv)) {
            // Private key may be absent on validator services; try default for dev issuer
            priv = readClasspath("/cdb-jwt-keys/private.pem");
        }
        return new JwtTokenService(priv, pub, keyId);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String readClasspath(String path) {
        try {
            ClassPathResource res = new ClassPathResource(path.startsWith("/") ? path.substring(1) : path);
            if (!res.exists()) return "";
            byte[] bytes = res.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ""; // fallback to empty if not found or unreadable
        }
    }
}
