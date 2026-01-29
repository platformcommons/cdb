package com.platformcommons.cdb.platform.api.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class ApiModelRequest {
    private String name;
    private String description;
    private String version;
    private String schema;
    private Map<String, Object> properties;
    private String category;
    private boolean isGlobal;
}