package com.platformcommons.cdb.auth.registry.repository;

import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProviderMappingRepository extends JpaRepository<UserProviderMapping, Long> {
    Optional<UserProviderMapping> findByUserIdAndProviderCodeAndStatus(Long userId, String providerCode, UserProviderMapping.MappingStatus status);
    List<UserProviderMapping> findByUserIdAndStatus(Long userId, UserProviderMapping.MappingStatus status);

    boolean existsByProviderId(Long providerId);
}
