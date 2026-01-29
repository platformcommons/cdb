package com.platformcommons.cdb.platform.provider.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;

/**
 * ProviderKey stores public key material and metadata for a Provider.
 * Only the public key is persisted. Private key is never stored; only a checksum is kept.
 */
@Entity
@Table(name = "cdb_provider_keys", indexes = {
        @Index(name = "idx_providerkey_provider", columnList = "provider_id"),
        @Index(name = "idx_providerkey_keyid", columnList = "key_id", unique = true),
        @Index(name = "idx_providerkey_type_status", columnList = "provider_id, key_type, key_status"),
        @Index(name = "idx_providerkey_unique_active", columnList = "provider_id, key_type, key_status, environment, client_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /** External stable identifier for the key (UUID string) */
    @Column(name = "key_id", nullable = false, unique = true, length = 64)
    private String keyId;

    /** Client identifier for key scoping */
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId = "default";

    /** Key type - ENCRYPTION or SIGNING */
    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private KeyType keyType;

    /** Key status - ACTIVE or NOT_ACTIVE */
    @Enumerated(EnumType.STRING)
    @Column(name = "key_status", nullable = false, length = 20)
    private KeyStatus keyStatus;

    /** Environment - PRODUCTION or SANDBOX */
    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private Environment environment = Environment.SANDBOX;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    /** Optional human readable title */
    @Column(length = 256)
    private String title;

    /** PEM encoded public key */
    @Lob
    @Column(name = "public_key_pem", nullable = false, columnDefinition = "TEXT",updatable = false)
    private String publicKeyPem;

    /** Encrypted private key (stored for both symmetric and asymmetric) */
    @Lob
    @Column(name = "private_key_pem", columnDefinition = "TEXT", updatable = false)
    private String privateKeyPem;

    /** SHA-256 checksum of the private key PEM (hex) to support validation without storing secret */
    @Column(name = "private_key_checksum", length = 128, nullable = false,updatable = false)
    private String privateKeyChecksum;

    /** Issue and expiry timestamps */
    @Column(name = "issued_at", nullable = false,updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    /** Comma separated scopes for minimal persistence; UI/services can treat as set */
    @Column(name = "scopes", length = 1024)
    private String scopesCsv;

    /** Convenience accessors for scopes as a Set */
    public Set<String> getScopes() {
        if (scopesCsv == null || scopesCsv.isBlank()) return java.util.Collections.emptySet();
        return java.util.Arrays.stream(scopesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public void setScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            this.scopesCsv = null;
        } else {
            this.scopesCsv = String.join(",", scopes);
        }
    }

    public enum KeyType {
        ENCRYPTION, SIGNING
    }

    public enum KeyStatus {
        ACTIVE, NOT_ACTIVE, PENDING_FOR_APPROVAL
    }

    public enum Environment {
        PRODUCTION, SANDBOX
    }
}
