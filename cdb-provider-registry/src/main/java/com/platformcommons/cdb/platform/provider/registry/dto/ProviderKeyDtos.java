package com.platformcommons.cdb.platform.provider.registry.dto;

import com.platformcommons.cdb.platform.provider.registry.model.ProviderKey;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;

public class ProviderKeyDtos {

    @Data
    public static class GenerateKeyRequest {
        private String title;
        @NotNull(message = "Key type is required")
        private ProviderKey.KeyType keyType;
        private ProviderKey.KeyStatus keyStatus = ProviderKey.KeyStatus.ACTIVE;
        private ProviderKey.Environment environment = ProviderKey.Environment.SANDBOX;
        @Size(max = 100, message = "Client ID cannot exceed 100 characters")
        private String clientId = "default";
        private List<String> scopes; // optional
        private Instant expiresAt; // optional
        @Value("${provider.key.algorithm:RSA}")  // default is RSA if not specified in config
        private String algorithm;
        @Value("${provider.key.size:4096}")  // default is 4096 if not specified in config
        private Integer keySize;
    }

    @Data
    public static class GenerateKeyResponse {
        private String keyId;
        private String publicKeyPem;
        private String privateKeyPem; // returned once
        private ProviderKey.KeyType keyType;
        private ProviderKey.KeyStatus keyStatus;
        private ProviderKey.Environment environment;
        private String clientId;
        private Instant issuedAt;
        private Instant expiresAt;
        private List<String> scopes;
        private String title;
    }

    @Data
    public static class KeySummary {
        private Long id;
        private String keyId;
        private String title;
        private ProviderKey.KeyType keyType;
        private ProviderKey.KeyStatus keyStatus;
        private ProviderKey.Environment environment;
        private String clientId;
        private Instant issuedAt;
        private Instant expiresAt;
        private List<String> scopes;
        private String publicKeyPem;
        private String privateKeyChecksum;
    }

    @Data
    public static class ValidateKeyRequest {
        private String keyId; // or public key to identify
        private String publicKeyPem; // optional
        private String privateKeyPem; // required for validation
    }

    @Data
    public static class ValidateKeyResponse {
        private String keyId;
        private boolean valid;
        private String reason;
    }

    public record PublicKeyResponse(String publicKey) {
    }
}
