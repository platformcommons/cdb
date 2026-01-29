package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.security.jwt.JwtTokenService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the JWKS (JSON Web Key Set) endpoint for asymmetric JWT verification.
 * When RS256 is configured in JwtTokenService, this endpoint returns the public key in JWKS format.
 * Location: /.well-known/jwks.json (also mapped to /jwks.json for convenience)
 */
@RestController
public class JwksController {

    private final JwtTokenService jwtTokenService;

    public JwksController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> wellKnownJwks() {
        Map<String, Object> jwks = jwtTokenService.getJwks();
        return ResponseEntity.ok(jwks);
    }

    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        Map<String, Object> jwks = jwtTokenService.getJwks();
        return ResponseEntity.ok(jwks);
    }
}
