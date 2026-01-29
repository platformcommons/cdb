package com.platformcommons.cdb.platform.provider.registry.dto;

import com.platformcommons.cdb.platform.provider.registry.model.ProviderEnvironment.EnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEnvironmentDto {
    private Long id;
    private Long providerId;
    private EnvironmentType environmentType;
    private String baseUrl;
    private String uptimeStatus;
    private Integer rateLimit;
    private String remarks;
}

