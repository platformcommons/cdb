package com.platformcommons.cdb.platform.api.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ApiEndpointRequest {
    private String name;
    private String description;
    private String path;
    private String method;
    private String requestModel;
    private String responseModel;
    private List<String> tags;
    private List<ApiParameterRequest> parameters;
    private String category;
    private boolean isGlobal;
}