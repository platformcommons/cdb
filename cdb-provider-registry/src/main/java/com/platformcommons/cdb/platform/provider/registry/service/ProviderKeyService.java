package com.platformcommons.cdb.platform.provider.registry.service;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderKeyDtos;
import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderKey;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderKeyRepository;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderRepository;
import com.platformcommons.cdb.security.auth.CDBSecurityUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.channels.FileChannel;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProviderKeyService {

    private static final Logger log = LoggerFactory.getLogger(ProviderKeyService.class);

    private final ProviderRepository providerRepository;
    private final ProviderKeyRepository providerKeyRepository;
    private final CDBSecurityUtil cDBSecurityUtil;

    public ProviderKeyService(ProviderRepository providerRepository, ProviderKeyRepository providerKeyRepository, CDBSecurityUtil cDBSecurityUtil) {
        this.providerRepository = providerRepository;
        this.providerKeyRepository = providerKeyRepository;
        this.cDBSecurityUtil = cDBSecurityUtil;
    }

    private static ProviderKeyDtos.GenerateKeyResponse getGenerateKeyResponse(ProviderKey saved, String privatePem) {
        ProviderKeyDtos.GenerateKeyResponse resp = new ProviderKeyDtos.GenerateKeyResponse();
        resp.setKeyId(saved.getKeyId());
        resp.setPublicKeyPem(saved.getPublicKeyPem());
        resp.setPrivateKeyPem(privatePem); // return once
        resp.setKeyType(saved.getKeyType());
        resp.setKeyStatus(saved.getKeyStatus());
        resp.setEnvironment(saved.getEnvironment());
        resp.setClientId(saved.getClientId());
        resp.setIssuedAt(saved.getIssuedAt());
        resp.setExpiresAt(saved.getExpiresAt());
        resp.setScopes(new ArrayList<>(saved.getScopes()));
        resp.setTitle(saved.getTitle());
        return resp;
    }

    private static String toPem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available for checksum calculation", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<ProviderKeyDtos.GenerateKeyResponse> generateSymmetricKey(Long providerId,
                                                                              ProviderKeyDtos.@Valid
                                                                                      GenerateKeyRequest request) {
        request.setClientId(cDBSecurityUtil.getCurrentUserLogin());
        //todo - from provider config
        request.setKeyStatus(ProviderKey.KeyStatus.ACTIVE);
        request.setKeyType(ProviderKey.KeyType.ENCRYPTION);
        return generateKey(providerId, request);
    }

    public Optional<ProviderKeyDtos.GenerateKeyResponse> generateKey(Long providerId, ProviderKeyDtos.GenerateKeyRequest request) {
        Provider provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) return Optional.empty();
        if (request.getKeyStatus() == null) {
            request.setKeyStatus(ProviderKey.KeyStatus.ACTIVE);
        }
        // Validate unique active key per type, environment and client_id
        String clientId = request.getClientId() == null || request.getClientId().trim().isEmpty() ? "default" : request.getClientId().trim();
        if ((request.getKeyStatus() == ProviderKey.KeyStatus.ACTIVE
                || request.getKeyStatus() == ProviderKey.KeyStatus.PENDING_FOR_APPROVAL)) {
            List<ProviderKey> response = providerKeyRepository.findByProvider_IdAndKeyTypeAndKeyStatusInAndEnvironmentAndClientId(providerId,
                    request.getKeyType(),
                    Set.of(ProviderKey.KeyStatus.ACTIVE, ProviderKey.KeyStatus.PENDING_FOR_APPROVAL), request.getEnvironment(), clientId);
            if (response != null && !response.isEmpty()) {
                ProviderKey existing = response.getFirst();
                if (existing.getKeyType() == ProviderKey.KeyType.SIGNING) {
                    throw new IllegalArgumentException("An active key of type " +
                            request.getKeyType() + " already exists for this provider in "
                            + request.getEnvironment() + " environment for client " + clientId);
                } else {
                    return Optional.of(getGenerateKeyResponse(existing,
                            existing.getPrivateKeyPem()));
                }
            }

        }

        try {
            String publicPem;
            String privatePem;
            String privateKeyToReturn;

            if (request.getKeyType() == ProviderKey.KeyType.SIGNING) {
                // Asymmetric key generation for SIGNING
                String algorithm = request.getAlgorithm() == null ? "RSA" : request.getAlgorithm();
                int keySize = request.getKeySize() == null ? 4096 : request.getKeySize();
                if (!"RSA".equalsIgnoreCase(algorithm)) {
                    throw new IllegalArgumentException("Only RSA supported for asymmetric keys");
                }

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(keySize);
                KeyPair kp = kpg.generateKeyPair();
                RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
                RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();

                publicPem = toPem("PUBLIC KEY", pub.getEncoded());
                privatePem = toPem("PRIVATE KEY", priv.getEncoded());
                privateKeyToReturn = privatePem; // Don't return private key for asymmetric
            } else {
                // Symmetric key generation for ENCRYPTION
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256); // AES-256
                SecretKey secretKey = keyGen.generateKey();

                byte[] keyBytes = secretKey.getEncoded();
                String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);

                publicPem = keyBase64; // For symmetric, "public" is the key itself
                privatePem = keyBase64; // Same as public for symmetric
                privateKeyToReturn = keyBase64; // Return the symmetric key
            }

            String checksum = sha256Hex(privatePem.getBytes());

            ProviderKey pk = ProviderKey.builder()
                    .keyId(UUID.randomUUID().toString())
                    .provider(provider)
                    .title(request.getTitle())
                    .keyType(request.getKeyType())
                    .keyStatus(request.getKeyStatus())
                    .environment(request.getEnvironment())
                    .clientId(clientId)
                    .publicKeyPem(publicPem)
                    .privateKeyPem(privatePem)
                    .privateKeyChecksum(checksum)
                    .issuedAt(Instant.now())
                    .expiresAt(request.getExpiresAt())
                    .build();
            if (request.getScopes() != null) {
                pk.setScopes(new java.util.LinkedHashSet<>(request.getScopes()));
            }
            ProviderKey saved = providerKeyRepository.save(pk);
            ProviderKeyDtos.GenerateKeyResponse resp = getGenerateKeyResponse(saved, privateKeyToReturn);
            return Optional.of(resp);
        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not available for key generation", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    public List<ProviderKeyDtos.KeySummary> listKeys(Long providerId) {
        return providerKeyRepository.findByProvider_Id(providerId).stream().map(pk -> {
            ProviderKeyDtos.KeySummary s = new ProviderKeyDtos.KeySummary();
            s.setId(pk.getId());
            s.setKeyId(pk.getKeyId());
            s.setTitle(pk.getTitle());
            s.setKeyType(pk.getKeyType());
            s.setKeyStatus(pk.getKeyStatus());
            s.setEnvironment(pk.getEnvironment());
            s.setClientId(pk.getClientId());
            s.setIssuedAt(pk.getIssuedAt());
            s.setExpiresAt(pk.getExpiresAt());
            s.setScopes(new ArrayList<>(pk.getScopes()));
            s.setPublicKeyPem(pk.getPublicKeyPem());
            s.setPrivateKeyChecksum(pk.getPrivateKeyChecksum());
            return s;
        }).collect(Collectors.toList());
    }


    public List<ProviderKeyDtos.KeySummary> listKeysPublic(Long providerId) {
        return providerKeyRepository.findByProvider_IdAndKeyTypeAndKeyStatus(providerId,
                ProviderKey.KeyType.SIGNING, ProviderKey.KeyStatus.ACTIVE).stream().map(pk -> {
            ProviderKeyDtos.KeySummary s = new ProviderKeyDtos.KeySummary();
            s.setEnvironment(pk.getEnvironment());
            s.setPublicKeyPem(pk.getPublicKeyPem());
            return s;
        }).collect(Collectors.toList());
    }

    private String normalizePem(String pem) {
        return pem.replace("\r", "").trim();
    }

    public Optional<String> getPublicKeyByProviderCodeAndKeyType(String providerCode, ProviderKey.KeyType keyType) {
        return providerKeyRepository.findActiveNonExpiredByProviderCodeAndKeyType(providerCode, keyType, Instant.now())
                .map(ProviderKey::getPublicKeyPem);
    }

    public boolean deactivateKey(Long providerId, String keyId) {
        Optional<ProviderKey> keyOpt = providerKeyRepository.findByProvider_IdAndKeyId(providerId, keyId);
        if (keyOpt.isEmpty()) {
            return false;
        }

        ProviderKey key = keyOpt.get();
        key.setKeyStatus(ProviderKey.KeyStatus.NOT_ACTIVE);
        providerKeyRepository.save(key);
        return true;
    }


}
