package com.platformcommons.cdb.platform.provider.registry.repository;

import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Provider persistence operations.
 *
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByCode(String code);
    List<Provider> findByNameContainingIgnoreCase(String name);
    List<Provider> findByStatus(String status);
    List<Provider> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
}
