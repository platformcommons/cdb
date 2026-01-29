package com.platformcommons.cdb.auth.registry.service.impl;

import com.platformcommons.cdb.auth.registry.dto.TokenResponse;
import com.platformcommons.cdb.auth.registry.model.OAuth2AuthorizationCode;
import com.platformcommons.cdb.auth.registry.model.OAuth2Client;
import com.platformcommons.cdb.auth.registry.model.User;
import com.platformcommons.cdb.auth.registry.repository.OAuth2AuthorizationCodeRepository;
import com.platformcommons.cdb.auth.registry.repository.OAuth2ClientRepository;
import com.platformcommons.cdb.auth.registry.repository.UserRepository;
import com.platformcommons.cdb.auth.registry.service.OAuth2Service;
import com.platformcommons.cdb.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private final OAuth2ClientRepository clientRepository;
    private final OAuth2AuthorizationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    @Value("${cdb.auth.oauth2.access-ttl:86400}")
    private Long accessTtl;

    public OAuth2ServiceImpl(OAuth2ClientRepository clientRepository,
                             OAuth2AuthorizationCodeRepository codeRepository,
                             UserRepository userRepository,
                             JwtTokenService jwtTokenService) {
        this.clientRepository = clientRepository;
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public OAuth2Client validateClient(String clientId, String redirectUri) {
        OAuth2Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

        if (!client.getRedirectUris().contains(redirectUri)) {
            throw new IllegalArgumentException("Invalid redirect URI");
        }

        return client;
    }

    @Override
    public boolean authenticate(String username, String password) {
        return userRepository.findByEmail(username.toLowerCase())
                .filter(User::isEnabled)
                .map(user -> encoder.matches(password, user.getPasswordHash()))
                .orElse(false);
    }

    @Override
    public String generateAuthorizationCode(String clientId, String username, String redirectUri,
                                            String scope, String codeChallenge, String codeChallengeMethod) {
        User user = userRepository.findByEmail(username.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String code = generateRandomCode();

        OAuth2AuthorizationCode authCode = OAuth2AuthorizationCode.builder()
                .code(code)
                .clientId(clientId)
                .userId(user.getId())
                .redirectUri(redirectUri)
                .scope(scope)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .build();

        codeRepository.save(authCode);
        return code;
    }

    @Override
    public TokenResponse exchangeCodeForToken(String code, String clientId, String codeVerifier) {
        OAuth2AuthorizationCode authCode = codeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid authorization code"));

        if (authCode.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Authorization code expired");
        }

        if (!authCode.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("Client mismatch");
        }

        // Verify PKCE if present
        if (authCode.getCodeChallenge() != null) {
            if (!verifyPkce(codeVerifier, authCode.getCodeChallenge(), authCode.getCodeChallengeMethod())) {
                throw new IllegalArgumentException("PKCE verification failed");
            }
        }

        // Mark code as used
        authCode.setUsed(true);
        codeRepository.save(authCode);

        // Generate access token
        User user = userRepository.findById(authCode.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", authCode.getScope());
        claims.put("client_id", clientId);
        claims.put("ctx", userContext(user));
        String accessToken =  jwtTokenService.generate(user.getEmail(), user.getId(),
                accessTtl, claims);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTtl)
                .build();

    }

    @Override
    public void sendPasswordResetEmail(String email) {
        // todo Implementation would send actual email
        System.out.println("Password reset email sent to: " + email);
    }

    private boolean verifyPkce(String codeVerifier, String codeChallenge, String method) {
        if (codeVerifier == null) return false;

        try {
            String computed;
            if ("S256".equals(method)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
                computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } else {
                computed = codeVerifier;
            }
            return computed.equals(codeChallenge);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateRandomCode() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }



    private Map<String, Object> userContext(User user) {
        Map<String, Object> ctx = new HashMap<>();
        Map<String, Object> userCtx = new HashMap<>();
        userCtx.put("id", user.getId());
        userCtx.put("login", user.getEmail());
        userCtx.put("username", user.getUsername());
        ctx.put("user", userCtx);
        return ctx;
    }
}