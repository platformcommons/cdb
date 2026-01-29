package com.platformcommons.cdb.platform.provider.registry.repository;

import com.platformcommons.cdb.platform.provider.registry.model.ProviderKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProviderKeyRepository extends JpaRepository<ProviderKey, Long> {

    List<ProviderKey> findByProvider_Id(Long providerId);
    Optional<ProviderKey> findByProvider_IdAndKeyId(Long providerId, String keyId);
    List<ProviderKey> findByProvider_IdAndKeyTypeAndKeyStatus(Long providerId, ProviderKey.KeyType keyType, ProviderKey.KeyStatus keyStatus);


    @Query("SELECT pk FROM ProviderKey pk WHERE pk.provider.id = :providerId " +
            "AND pk.keyType = :keyType AND pk.keyStatus IN :keyStatus " +
            "AND pk.environment = :environment AND pk.clientId = :clientId")
    List<ProviderKey> findByProvider_IdAndKeyTypeAndKeyStatusInAndEnvironmentAndClientId(
            @Param("providerId") Long providerId,
            @Param("keyType") ProviderKey.KeyType keyType,
            @Param("keyStatus") Collection<ProviderKey.KeyStatus> keyStatus,
            @Param("environment") ProviderKey.Environment environment,
            @Param("clientId") String clientId);

    @Query("SELECT pk FROM ProviderKey pk WHERE pk.provider.code = :providerCode " +
           "AND pk.keyType = :keyType AND pk.keyStatus = 'ACTIVE' " +
           "AND (pk.expiresAt IS NULL OR pk.expiresAt > :now)")
    Optional<ProviderKey> findActiveNonExpiredByProviderCodeAndKeyType(
        @Param("providerCode") String providerCode, 
        @Param("keyType") ProviderKey.KeyType keyType, 
        @Param("now") Instant now);
}
