package com.platformcommons.cdb.auth.registry.service;

import com.platformcommons.cdb.auth.registry.dto.TokenResponse;
import com.platformcommons.cdb.auth.registry.model.OAuth2Client;

public interface OAuth2Service {
    OAuth2Client validateClient(String clientId, String redirectUri);
    boolean authenticate(String username, String password);
    String generateAuthorizationCode(String clientId, String username, String redirectUri, 
                                   String scope, String codeChallenge, String codeChallengeMethod);
    TokenResponse exchangeCodeForToken(String code, String clientId, String codeVerifier);
    void sendPasswordResetEmail(String email);
}