package com.platformcommons.cdb.platform.provider.registry.repository;

import com.platformcommons.cdb.platform.provider.registry.model.ProviderRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRequestRepository extends JpaRepository<ProviderRequest, Long> {
    
    List<ProviderRequest> findByProviderIdAndStatus(Long providerId, String status);
    
    List<ProviderRequest> findByUserIdAndStatus(Long userId, String status);
    
    Optional<ProviderRequest> findByUserIdAndProviderId(Long userId, Long providerId);
    
    boolean existsByUserIdAndProviderIdAndStatus(Long userId, Long providerId, String status);
}