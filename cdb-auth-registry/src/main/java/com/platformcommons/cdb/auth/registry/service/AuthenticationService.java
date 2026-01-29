package com.platformcommons.cdb.auth.registry.service;

import com.platformcommons.cdb.auth.registry.dto.AuthenticationRequest;
import com.platformcommons.cdb.auth.registry.dto.ExecutiveContextRequest;
import com.platformcommons.cdb.auth.registry.dto.TokenResponse;

/**
 * Authentication Service
 * 
 * Service for handling user authentication and JWT token generation.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 */
public interface AuthenticationService {
    
    /**
     * Authenticate user and generate JWT tokens
     * @param request the authentication request
     * @return token response with access and refresh tokens
     */
    TokenResponse authenticate(AuthenticationRequest request);
    

    
    /**
     * Validate JWT token
     * @param token the JWT token to validate
     * @return true if token is valid
     */
    boolean validateToken(String token);
    
    /**
     * Extract username from JWT token
     * @param token the JWT token
     * @return username from token
     */
    String getEmailFromToken(String token);
    
    /**
     * Logout user and invalidate tokens
     * @param token the JWT token to invalidate
     */
    void logout(String token);

    /**
     * Issue an executive context token for the logged-in user using the given provider code
     * and optional role/authority codes. The current access token is used to identify the user.
     * @param currentAccessToken current bearer access token ("Bearer ..." or raw token)
     * @param request provider and roles/authorities details
     * @return new TokenResponse containing context-bound JWT
     */
    TokenResponse issueExecutiveContextToken(String currentAccessToken, ExecutiveContextRequest request);
}