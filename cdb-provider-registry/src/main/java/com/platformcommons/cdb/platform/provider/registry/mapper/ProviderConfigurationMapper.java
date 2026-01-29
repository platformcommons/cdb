package com.platformcommons.cdb.platform.provider.registry.mapper;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigDataDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigurationDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigurationRequest;
import com.platformcommons.cdb.platform.provider.registry.model.ConfigDataType;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderConfigData;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
public class ProviderConfigurationMapper {

    private void validateConfigValue(String value, ConfigDataType dataType) {
        if (value == null || dataType == null) return;

        try {
            switch (dataType.name().toLowerCase()) {
                case "double" -> Double.parseDouble(value);
                case "number" -> Integer.parseInt(value);
                case "boolean" -> Boolean.parseBoolean(value);
                case "string" -> {
                    if (value.isEmpty()) {
                        throw new IllegalArgumentException("String value cannot be empty");
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for type " + dataType + ": " + value);
        }
    }

    public ProviderConfigurationDto toDto(ProviderConfiguration entity) {
        if (entity == null) return null;

        return ProviderConfigurationDto.builder()
                .id(entity.getId())
                .configCode(entity.getConfigCode())
                .configLabel(entity.getConfigLabel())
                .configValue(entity.getConfigValue())
                .status(entity.getStatus())
                .visibility(entity.getVisibility())
                .configDataType(entity.getConfigDataType())
                .hasList(entity.getHasList())
                .providerId(entity.getProviderId())
                .configDataList(entity.getConfigDataList() != null ?
                        entity.getConfigDataList().stream()
                                .map(this::toConfigDataDto)
                                .toList() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ProviderConfiguration toEntity(ProviderConfigurationRequest request, Long providerId) {
        if (request == null) return null;
        if (request.getHasList() && request.getConfigValueList() != null) {
            request.getConfigValueList().forEach(value ->
                    validateConfigValue(value, request.getConfigDataType()));
        } else {
            validateConfigValue(request.getConfigValue(), request.getConfigDataType());
        }

        ProviderConfiguration entity = ProviderConfiguration.builder()
                .configCode(request.getConfigCode())
                .configLabel(request.getConfigLabel())
                .configValue(request.getConfigValue())
                .status(request.getStatus())
                .visibility(request.getVisibility())
                .configDataType(request.getConfigDataType())
                .hasList(request.getHasList())
                .providerId(providerId)
                .build();

        if (request.getHasList() && request.getConfigValueList() != null) {
            List<ProviderConfigData> configDataList = IntStream.range(0, request.getConfigValueList().size())
                    .mapToObj(i -> ProviderConfigData.builder()
                            .configValue(request.getConfigValueList().get(i))
                            .configValueSequence(i + 1)
                            .providerConfiguration(entity)
                            .build())
                    .toList();
            entity.setConfigDataList(configDataList);
        }

        return entity;
    }

    private ProviderConfigDataDto toConfigDataDto(ProviderConfigData entity) {
        return ProviderConfigDataDto.builder()
                .id(entity.getId())
                .configValue(entity.getConfigValue())
                .configValueSequence(entity.getConfigValueSequence())
                .build();
    }
}