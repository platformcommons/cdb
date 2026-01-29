package com.platformcommons.cdb.auth.registry.service.impl;

import com.platformcommons.cdb.auth.registry.dto.AuthenticationRequest;
import com.platformcommons.cdb.auth.registry.dto.ExecutiveContextRequest;
import com.platformcommons.cdb.auth.registry.dto.TokenResponse;
import com.platformcommons.cdb.auth.registry.model.AuthorityMaster;
import com.platformcommons.cdb.auth.registry.model.RoleMaster;
import com.platformcommons.cdb.auth.registry.model.User;
import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;
import com.platformcommons.cdb.auth.registry.repository.AuthorityMasterRepository;
import com.platformcommons.cdb.auth.registry.repository.RoleMasterRepository;
import com.platformcommons.cdb.auth.registry.repository.UserProviderMappingRepository;
import com.platformcommons.cdb.auth.registry.repository.UserRepository;
import com.platformcommons.cdb.auth.registry.service.AuthenticationService;
import com.platformcommons.cdb.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Authentication service that validates users against database and
 * issues JWT access tokens with refresh token support.
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final UserProviderMappingRepository userProviderMappingRepository;
    private final RoleMasterRepository roleMasterRepository;
    private final AuthorityMasterRepository authorityMasterRepository;
    private final JwtTokenService jwtTokenService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final Map<String, TokenMeta> refreshTokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Value("${cdb.auth.jwt.access-ttl:86400}")
    private Long accessTtl;

    @Value("${cdb.auth.jwt.refresh-ttl:864000}")
    private Long refreshTtl;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     UserProviderMappingRepository userProviderMappingRepository,
                                     RoleMasterRepository roleMasterRepository,
                                     AuthorityMasterRepository authorityMasterRepository,
                                     JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.userProviderMappingRepository = userProviderMappingRepository;
        this.roleMasterRepository = roleMasterRepository;
        this.authorityMasterRepository = authorityMasterRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public TokenResponse authenticate(AuthenticationRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("email and password are required");
        }
        String email = normalize(request.getEmail());
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!user.isEnabled()) {
            throw new IllegalStateException("User disabled");
        }
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        // issue JWT access token
        Map<String, Object> ctx = userContext(user);
        String access = generateJwtTokenWithContext(email, user.getId(), ctx);

        return TokenResponse.builder()
                .accessToken(access)
                .tokenType("Bearer")
                .expiresIn(accessTtl)
                .build();
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

    @Override
    public boolean validateToken(String token) {
        return jwtTokenService.validate(token);
    }

    @Override
    public String getEmailFromToken(String token) {
        try {
            return jwtTokenService.getSubject(token);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void logout(String token) {
        // For JWT, we'd typically add to a blacklist or use short expiry
        // For now, just remove any associated refresh tokens
        String username = getEmailFromToken(token);
        if (username != null) {
            refreshTokens.entrySet().removeIf(entry ->
                    username.equals(entry.getValue().email));
        }
    }

    @Override
    public TokenResponse issueExecutiveContextToken(String currentAccessToken,
                                                    ExecutiveContextRequest request) {
        if (request == null || request.getProviderCode() == null || request.getProviderCode().isBlank()) {
            throw new IllegalArgumentException("providerCode is required");
        }
        String token = currentAccessToken;
        if (token != null && token.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            token = token.substring(7);
        }
        if (token == null || !validateToken(token)) {
            throw new IllegalArgumentException("Invalid or missing access token");
        }
        String username = getEmailFromToken(token);
        if (username == null) {
            throw new IllegalArgumentException("Invalid token subject");
        }
        User user = userRepository.findByEmail(normalize(username))
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        // Validate active mapping
        UserProviderMapping mapping = userProviderMappingRepository
                .findByUserIdAndProviderCodeAndStatus(user.getId(), request.getProviderCode(), UserProviderMapping.MappingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active mapping for provider code"));

        // Resolve role codes and authority codes from master tables. Unknown codes are ignored for minimal implementation.
        List<String> roleCodes = new ArrayList<>();
        List<String> authorityCodes =new ArrayList<>();

        if(mapping.getRoles() != null && !mapping.getRoles().isEmpty()) {
            mapping.getRoles().forEach(roleMaster -> {
                roleCodes.add(roleMaster.getCode());
                if(roleMaster.getAuthorities()!=null && !roleMaster.getAuthorities().isEmpty()) {
                    roleMaster.getAuthorities().forEach(authorityMaster
                            -> authorityCodes.add(authorityMaster.getCode()));
                }
            });
        }

        // Build claims
        Map<String, Object> ctx = new HashMap<>();
        Map<String, Object> userCtx = userContext(user);
        Map<String, Object> providerCtx = new HashMap<>();
        providerCtx.put("id", mapping.getProviderId());
        providerCtx.put("code", mapping.getProviderCode());
        ctx.put("user", userCtx);
        ctx.put("provider", providerCtx);
        ctx.put("roles", roleCodes);
        ctx.put("authorities", authorityCodes);

        String contextToken = generateJwtTokenWithContext(user.getUsername(), user.getId(),
                ctx);
        return TokenResponse.builder()
                .accessToken(contextToken)
                .tokenType("Bearer")
                .expiresIn(accessTtl)
                .build();
    }

    private String generateJwtTokenWithContext(String email, Long userId,
                                               Map<String, Object> contextClaims) {
        Map<String, Object> extra = null;
        if (contextClaims != null) {
            extra = new HashMap<>();
            extra.put("ctx", contextClaims);
        }
        return jwtTokenService.generate(email, userId, accessTtl, extra);
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    private static class TokenMeta {
        final String email;
        final Instant expiry;

        TokenMeta(String email, Instant expiry) {
            this.email = email;
            this.expiry = expiry;
        }
    }
}
