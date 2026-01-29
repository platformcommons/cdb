package com.platformcommons.cdb.platform.provider.registry.dto;

import com.platformcommons.cdb.platform.provider.registry.model.ConfigDataType;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigStatus;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfigurationRequest {
    @NotBlank
    private String configCode;
    
    @NotBlank
    private String configLabel;
    
    private String configValue;
    
    @NotNull
    private ConfigStatus status;
    
    @NotNull
    private ConfigVisibility visibility;
    
    @NotNull
    private ConfigDataType configDataType;
    
    @NotNull
    private Boolean hasList;
    
    private List<String> configValueList;
}