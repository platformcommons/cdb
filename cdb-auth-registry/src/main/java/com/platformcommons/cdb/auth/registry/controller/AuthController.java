package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.dto.AuthenticationRequest;
import com.platformcommons.cdb.auth.registry.dto.ExecutiveContextRequest;
import com.platformcommons.cdb.auth.registry.dto.TokenResponse;
import com.platformcommons.cdb.auth.registry.dto.ProviderContextOption;
import com.platformcommons.cdb.auth.registry.model.User;
import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;
import com.platformcommons.cdb.auth.registry.service.AuthenticationService;
import com.platformcommons.cdb.auth.registry.service.UserManagementService;
import com.platformcommons.cdb.auth.registry.service.UserProviderMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Authentication Controller
 * 
 * REST controller for authentication operations including login, token refresh,
 * and OAuth2 authorization flows.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    private final UserManagementService userManagementService;
    private final UserProviderMappingService userProviderMappingService;

    public AuthController(AuthenticationService authenticationService,
                          UserManagementService userManagementService, UserProviderMappingService userProviderMappingService) {
        this.authenticationService = authenticationService;
        this.userManagementService = userManagementService;
        this.userProviderMappingService = userProviderMappingService;
    }
    
    /**
     * Authenticate user and return JWT tokens
     * @param request authentication request
     * @return token response with access and refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            TokenResponse tokenResponse = authenticationService.authenticate(request);
            return ResponseEntity.ok(tokenResponse);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(401).body("Invalid user credentials");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Authentication failed");
        }
    }

    /**
     * Validate JWT token
     * @param token the JWT token to validate
     * @return validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        boolean isValid = authenticationService.validateToken(token);
        return ResponseEntity.ok(isValid);
    }
    
    /**
     * Logout user and invalidate tokens
     * @param token the JWT token to invalidate
     * @return success response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String token) {
        authenticationService.logout(token);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Set executive context for the logged-in user using provider code input
     * and optional role/authority codes. Returns a new token containing the context.
     */
    @PostMapping("/context")
    public ResponseEntity<TokenResponse> setExecutiveContext(@RequestHeader("Authorization") String authorization,
                                                             @RequestBody ExecutiveContextRequest request) {
        TokenResponse tokenResponse = authenticationService.issueExecutiveContextToken(authorization, request);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * List provider IDs and codes mapped to the logged-in user (ACTIVE mappings only).
     */
    @GetMapping("/my-providers")
    public ResponseEntity<List<ProviderContextOption>> listMyProviders(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization;
        if (token != null && token.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            token = token.substring(7);
        }
        if (token == null || !authenticationService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        String email = authenticationService.getEmailFromToken(token);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userManagementService.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        List<UserProviderMapping> mappings = userProviderMappingService.findByUserIdAndStatus(user.getId(), UserProviderMapping.MappingStatus.ACTIVE);
        List<ProviderContextOption> result = mappings.stream()
                .map(m -> new ProviderContextOption(m.getProviderId(), m.getProviderCode()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

}