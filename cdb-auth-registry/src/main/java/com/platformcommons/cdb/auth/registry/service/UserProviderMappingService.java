package com.platformcommons.cdb.auth.registry.service;

import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing User-Provider mappings.
 */
public interface UserProviderMappingService {

    UserProviderMapping create(UserProviderMapping mapping);

    UserProviderMapping updateStatus(Long id, UserProviderMapping.MappingStatus status);

    boolean delete(Long id);

    Optional<UserProviderMapping> findByUserIdAndProviderCodeAndStatus(Long userId, String providerCode, UserProviderMapping.MappingStatus status);
    List<UserProviderMapping> findByUserIdAndStatus(Long userId, UserProviderMapping.MappingStatus status);
}
