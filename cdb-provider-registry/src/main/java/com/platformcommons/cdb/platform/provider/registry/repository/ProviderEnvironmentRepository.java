package com.platformcommons.cdb.platform.provider.registry.repository;

import com.platformcommons.cdb.platform.provider.registry.model.ProviderEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProviderEnvironmentRepository extends JpaRepository<ProviderEnvironment, Long> {
    List<ProviderEnvironment> findByProviderId(Long providerId);
}

