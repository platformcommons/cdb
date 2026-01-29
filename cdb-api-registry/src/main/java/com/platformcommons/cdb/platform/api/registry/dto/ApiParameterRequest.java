package com.platformcommons.cdb.platform.api.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiParameterRequest {
    private String name;
    private String location;
    private String type;
    private String description;
    private String defaultValue;
    private String example;
    private Integer minLength;
    private Integer maxLength;
    private boolean required;
    private boolean deprecated;
    private boolean allowEmpty;
}