package com.platformcommons.cdb.platform.provider.registry.service;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderEnvironmentDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderEnvironmentRequest;
import com.platformcommons.cdb.platform.provider.registry.mapper.ProviderEnvironmentMapper;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderEnvironment;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderEnvironmentRepository;
import com.platformcommons.cdb.security.auth.CDBSecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProviderEnvironmentService {
    private final ProviderEnvironmentRepository repository;
    private final ProviderEnvironmentMapper mapper;
    private final CDBSecurityUtil cdbSecurityUtil;
    public ProviderEnvironmentService(ProviderEnvironmentRepository repository, ProviderEnvironmentMapper mapper, CDBSecurityUtil cdbSecurityUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.cdbSecurityUtil = cdbSecurityUtil;
    }

    public ProviderEnvironmentDto create(ProviderEnvironmentRequest request) {
        request.setProviderId(cdbSecurityUtil.getCurrentProviderId());
        ProviderEnvironment entity = mapper.toEntity(request);
        ProviderEnvironment saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public Optional<ProviderEnvironmentDto> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public List<ProviderEnvironmentDto> findByProviderId(Long providerId) {
        providerId = cdbSecurityUtil.getCurrentProviderId();
        return repository.findByProviderId(providerId).stream().map(mapper::toDto).toList();
    }

    public Optional<ProviderEnvironmentDto> update(Long id, ProviderEnvironmentRequest request) {

        return repository.findById(id).map(existing -> {
            Long currentProviderId = cdbSecurityUtil.getCurrentProviderId();
            if (!existing.getProviderId().equals(currentProviderId)) {
                throw new IllegalStateException("Cannot update environment: providerId mismatch");
            }
            existing.setEnvironmentType(request.getEnvironmentType());
            existing.setBaseUrl(request.getBaseUrl());
            existing.setUptimeStatus(request.getUptimeStatus());
            existing.setRateLimit(request.getRateLimit());
            existing.setRemarks(request.getRemarks());
            return mapper.toDto(repository.save(existing));
        });
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(existing -> {
            Long currentProviderId = cdbSecurityUtil.getCurrentProviderId();
            if (!existing.getProviderId().equals(currentProviderId)) {
                throw new IllegalStateException("Cannot update environment: providerId mismatch");
            }
            repository.delete(existing);
            return true;
        }).orElse(false);
    }
}

