package com.platformcommons.cdb.platform.provider.registry.dto;

import com.platformcommons.cdb.platform.provider.registry.model.ConfigDataType;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigStatus;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigVisibility;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfigurationDto {
    private Long id;
    private String configCode;
    private String configLabel;
    private String configValue;
    private ConfigStatus status;
    private ConfigVisibility visibility;
    private ConfigDataType configDataType;
    private Boolean hasList;
    private Long providerId;
    private List<ProviderConfigDataDto> configDataList;
    private Instant createdAt;
    private Instant updatedAt;
}

