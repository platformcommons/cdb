package com.platformcommons.cdb.platform.provider.registry.service;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigurationDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigurationRequest;
import com.platformcommons.cdb.platform.provider.registry.mapper.ProviderConfigurationMapper;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigStatus;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigVisibility;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderConfiguration;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProviderConfigurationService {

    private final ProviderConfigurationRepository repository;
    private final ProviderConfigurationMapper mapper;

    public ProviderConfigurationService(ProviderConfigurationRepository repository, 
                                      ProviderConfigurationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ProviderConfigurationDto create(ProviderConfigurationRequest request, Long providerId) {
        validateProviderConfiguration(request, providerId, null);
        ProviderConfiguration entity = mapper.toEntity(request, providerId);
        ProviderConfiguration saved = repository.save(entity);
        return mapper.toDto(saved);
    }
    
    private void validateProviderConfiguration(ProviderConfigurationRequest request, Long providerId, Long excludeId) {
        if (request.getStatus() == ConfigStatus.ACTIVE) {
            Optional<ProviderConfiguration> existing = repository.findByProviderIdAndConfigCodeAndStatus(
                providerId, request.getConfigCode(), ConfigStatus.ACTIVE);
            
            if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
                throw new IllegalArgumentException(
                    "An active configuration with code '" + request.getConfigCode() + 
                    "' already exists for this provider. Please deactivate the existing configuration first.");
            }
        }
    }

    public Optional<ProviderConfigurationDto> findById(Long id, Long providerId) {
        return repository.findById(id)
                .filter(config -> config.getProviderId().equals(providerId) && config.getStatus() == ConfigStatus.ACTIVE)
                .map(mapper::toDto);
    }

    public List<ProviderConfigurationDto> findByProviderId(Long providerId) {
        return repository.findByProviderIdAndStatus(providerId, ConfigStatus.ACTIVE)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public Optional<ProviderConfigurationDto> update(Long id, ProviderConfigurationRequest request, Long providerId) {
        return repository.findById(id)
                .filter(config -> config.getProviderId().equals(providerId))
                .map(existing -> {
                    validateProviderConfiguration(request, providerId, id);
                    
                    existing.setConfigCode(request.getConfigCode());
                    existing.setConfigLabel(request.getConfigLabel());
                    existing.setConfigValue(request.getConfigValue());
                    existing.setStatus(request.getStatus());
                    existing.setVisibility(request.getVisibility());
                    existing.setConfigDataType(request.getConfigDataType());
                    existing.setHasList(request.getHasList());
                    
                    existing.getConfigDataList().clear();
                    if (request.getHasList() && request.getConfigValueList() != null) {
                        for (int i = 0; i < request.getConfigValueList().size(); i++) {
                            existing.getConfigDataList().add(
                                com.platformcommons.cdb.platform.provider.registry.model.ProviderConfigData.builder()
                                    .configValue(request.getConfigValueList().get(i))
                                    .configValueSequence(i + 1)
                                    .providerConfiguration(existing)
                                    .build()
                            );
                        }
                    }
                    
                    return mapper.toDto(repository.save(existing));
                });
    }

    public boolean delete(Long id, Long providerId) {
        return repository.findById(id)
                .filter(config -> config.getProviderId().equals(providerId))
                .map(config -> {
                    repository.delete(config);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<ProviderConfigurationDto> findPublicConfig(String configCode, String providerCode) {
        return repository.findByConfigCodeAndProviderCodeAndVisibilityAndStatus(
                configCode, providerCode, ConfigVisibility.PUBLIC, ConfigStatus.ACTIVE)
                .map(mapper::toDto);
    }
}