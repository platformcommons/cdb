package com.platformcommons.cdb.platform.provider.registry.repository;

import com.platformcommons.cdb.platform.provider.registry.model.ConfigStatus;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigVisibility;
import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderConfigurationRepository extends JpaRepository<ProviderConfiguration, Long> {
    
    List<ProviderConfiguration> findByProviderIdAndStatus(Long providerId, ConfigStatus status);
    
    Optional<ProviderConfiguration> findByProviderIdAndConfigCodeAndStatus(Long providerId, String configCode, ConfigStatus status);
    
    @Query("SELECT pc FROM ProviderConfiguration pc JOIN Provider p ON pc.providerId = p.id " +
           "WHERE pc.configCode = :configCode AND p.code = :providerCode AND pc.visibility = :visibility AND pc.status = :status")
    Optional<ProviderConfiguration> findByConfigCodeAndProviderCodeAndVisibilityAndStatus(
            @Param("configCode") String configCode, 
            @Param("providerCode") String providerCode, 
            @Param("visibility") ConfigVisibility visibility,
            @Param("status") ConfigStatus status);
}