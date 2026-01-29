package com.platformcommons.cdb.platform.provider.registry.mapper;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderEnvironmentDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderEnvironmentRequest;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderEnvironment;
import org.springframework.stereotype.Component;

@Component
public class ProviderEnvironmentMapper {
    public ProviderEnvironment toEntity(ProviderEnvironmentRequest request) {
        ProviderEnvironment entity = new ProviderEnvironment();
        entity.setProviderId(request.getProviderId());
        entity.setEnvironmentType(request.getEnvironmentType());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setUptimeStatus(request.getUptimeStatus());
        entity.setRateLimit(request.getRateLimit());
        entity.setRemarks(request.getRemarks());
        return entity;
    }

    public ProviderEnvironmentDto toDto(ProviderEnvironment entity) {
        ProviderEnvironmentDto dto = new ProviderEnvironmentDto();
        dto.setId(entity.getId());
        dto.setProviderId(entity.getProviderId());
        dto.setEnvironmentType(entity.getEnvironmentType());
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setUptimeStatus(entity.getUptimeStatus());
        dto.setRateLimit(entity.getRateLimit());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }
}

