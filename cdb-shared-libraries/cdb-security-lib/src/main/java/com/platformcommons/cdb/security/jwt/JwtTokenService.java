package com.platformcommons.cdb.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Central JWT service for key management and common operations.
 * RS256-only (asymmetric) implementation.
 *
 * Configuration (see SecurityJwtAutoConfiguration):
 * - cdb.security.jwt.rsa.private-key (PEM PKCS#8, required on issuer)
 * - cdb.security.jwt.rsa.public-key (PEM X.509 SubjectPublicKeyInfo, required on validators)
 * - cdb.security.jwt.kid (optional, will be set in JWT header and JWKS)
 *
 * If keys are not configured via properties/env, defaults bundled in the library
 * will be used (development only). Override them in deployment via environment variables
 * or application.yml.
 */
public class JwtTokenService {

    private final PrivateKey rsaPrivate; // for RS256 signing (issuer)
    private final PublicKey rsaPublic;   // for RS256 verification (validators)
    private final String keyId;          // optional KID

    public JwtTokenService(String rsaPrivateKeyPem, String rsaPublicKeyPem, String keyId) {
        this.keyId = (keyId == null || keyId.isBlank()) ? null : keyId.trim();
        PrivateKey rpriv = null;
        PublicKey rpub = null;
        try {
            if (rsaPublicKeyPem != null && !rsaPublicKeyPem.isBlank()) {
                rpub = parsePublicKeyFromPem(rsaPublicKeyPem);
            }
            if (rsaPrivateKeyPem != null && !rsaPrivateKeyPem.isBlank()) {
                rpriv = parsePrivateKeyFromPem(rsaPrivateKeyPem);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA keys from PEM: " + e.getMessage(), e);
        }
        // Public key is mandatory for all services (validators and issuers)
        if (rpub == null) {
            throw new IllegalStateException("RSA public key (cdb.security.jwt.rsa.public-key) is required but was not provided or could not be parsed");
        }
        this.rsaPrivate = rpriv; // may be null on validator-only services
        this.rsaPublic = rpub;
    }

    public String generate(String subject, Long userId, Long ttl, Map<String, Object> extraClaims) {
        Date expiry = Date.from(Instant.now().plusSeconds(ttl));
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(expiry);
        if (extraClaims != null) {
            for (Map.Entry<String, Object> e : extraClaims.entrySet()) {
                builder.claim(e.getKey(), e.getValue());
            }
        }
        if (keyId != null && !keyId.isBlank()) {
            builder.header().keyId(keyId);
        }
        if (rsaPrivate == null) {
            throw new IllegalStateException("RS256 signing requested but private key is not configured on this service");
        }
        return builder.signWith(rsaPrivate).compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(rsaPublic).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(rsaPublic).build().parseSignedClaims(token).getPayload();
    }

    public String getSubject(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Return JWKS for RS256 configuration.
     */
    public Map<String, Object> getJwks() {
        Map<String, Object> jwks = new HashMap<>();
        if (rsaPublic instanceof RSAPublicKey rsa) {
            Map<String, Object> jwk = new HashMap<>();
            jwk.put("kty", "RSA");
            jwk.put("alg", "RS256");
            if (keyId != null) jwk.put("kid", keyId);
            jwk.put("use", "sig");
            jwk.put("n", base64Url(rsa.getModulus().toByteArray()));
            jwk.put("e", base64Url(rsa.getPublicExponent().toByteArray()));
            jwks.put("keys", java.util.List.of(jwk));
        } else {
            jwks.put("keys", java.util.List.of());
        }
        return jwks;
    }

    private static String base64Url(byte[] bytes) {
        // Ensure positive integer for modulus/exponent encoding
        byte[] normalized = bytes;
        if (bytes.length > 0 && bytes[0] == 0x00) {
            normalized = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(normalized);
    }

    private static PublicKey parsePublicKeyFromPem(String pem) throws Exception {
        String content = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = decodeBase64Lenient(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static PrivateKey parsePrivateKeyFromPem(String pem) throws Exception {
        String content = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = decodeBase64Lenient(content);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /**
     * Some PEM files may be copied with missing padding or line-wrap artifacts. Be lenient by
     * normalizing and padding to a 4-byte boundary before Base64-decoding.
     */
    private static byte[] decodeBase64Lenient(String base64NoWs) {
        String s = base64NoWs;
        // Remove any characters that are not valid Base64 alphabet to be defensive
        s = s.replaceAll("[^A-Za-z0-9+/=]", "");
        int mod = s.length() % 4;
        if (mod > 0) {
            s = s + "===".substring(0, 4 - mod);
        }
        return Base64.getDecoder().decode(s);
    }

    private static KeyPair generateDevKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA keypair: " + e.getMessage(), e);
        }
    }

}
