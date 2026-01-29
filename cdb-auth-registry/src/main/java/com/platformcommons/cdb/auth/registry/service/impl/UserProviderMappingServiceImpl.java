package com.platformcommons.cdb.auth.registry.service.impl;

import com.platformcommons.cdb.auth.registry.model.RoleMaster;
import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;
import com.platformcommons.cdb.auth.registry.repository.UserProviderMappingRepository;
import com.platformcommons.cdb.auth.registry.service.UserProviderMappingService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
public class UserProviderMappingServiceImpl implements UserProviderMappingService {

    private final UserProviderMappingRepository repository;

    public UserProviderMappingServiceImpl(UserProviderMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserProviderMapping create(UserProviderMapping mapping) {
        boolean exists = repository.existsByProviderId(mapping.getProviderId());
        if (!exists) {
            mapping.setStatus(UserProviderMapping.MappingStatus.ACTIVE);
            mapping.setRoles(Set.of(adminRole()));
        } else {
            mapping.setStatus(UserProviderMapping.MappingStatus.REQUESTED);
        }
        if (mapping.getMappedAt() == null) {
            mapping.setMappedAt(Instant.now());
        }
        return repository.save(mapping);
    }

    @Override
    public UserProviderMapping updateStatus(Long id, UserProviderMapping.MappingStatus status) {
        UserProviderMapping existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Mapping not found"));
        existing.setStatus(status);
        return repository.save(existing);
    }

    @Override
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Override
    public Optional<UserProviderMapping> findByUserIdAndProviderCodeAndStatus(Long userId, String providerCode, UserProviderMapping.MappingStatus status) {
        return repository.findByUserIdAndProviderCodeAndStatus(userId, providerCode, status);
    }

    @Override
    public List<UserProviderMapping> findByUserIdAndStatus(Long userId, UserProviderMapping.MappingStatus status) {
        return repository.findByUserIdAndStatus(userId, status);
    }

    private RoleMaster adminRole() {
        return RoleMaster.builder()
                .id(2L)
                .type("SYSTEM")
                .code("PROLE.PROVIDER_ADMIN").label("Provider Admin").build();
    }
}
