package com.platformcommons.cdb.platform.provider.registry.service;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderResponse;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderSearchCriteria;
import com.platformcommons.cdb.platform.provider.registry.mapper.ProviderMapper;
import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that supports discovery/search of registered providers.
 *
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@Service
public class ProviderDiscoveryService {

    private final ProviderRepository providerRepository;
    private final ProviderMapper providerMapper;
    public ProviderDiscoveryService(ProviderRepository providerRepository, ProviderMapper providerMapper) {
        this.providerRepository = providerRepository;
        this.providerMapper = providerMapper;
    }

    /**
     * Performs a very basic filtering based on criteria. For production, prefer Specifications/QueryDSL.
     */
    @Transactional(readOnly = true)
    public List<ProviderResponse> search(ProviderSearchCriteria criteria) {
        List<Provider> base;
        if (criteria.getNameContains() != null && !criteria.getNameContains().isBlank()) {
            base = providerRepository.findByNameContainingIgnoreCase(criteria.getNameContains());
        } else if (criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            base = providerRepository.findByStatus(criteria.getStatus());
        } else {
            base = providerRepository.findAll();
        }
        return base.stream().filter(p -> {
            if (criteria.getTag() == null || criteria.getTag().isBlank()) return true;
            return p.getMetadata() != null && p.getMetadata().getTags() != null &&
                ("," + p.getMetadata().getTags().toLowerCase() + ",").contains(
                    ("," + criteria.getTag().toLowerCase() + ","));
        }).map(this::toResponse).collect(Collectors.toList());
    }

    private ProviderResponse toResponse(Provider p) {
        return providerMapper.toResponse(p);
    }
}
