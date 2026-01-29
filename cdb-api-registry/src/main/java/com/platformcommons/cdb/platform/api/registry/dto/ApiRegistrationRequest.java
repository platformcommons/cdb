package com.platformcommons.cdb.platform.api.registry.dto;

import lombok.*;

import java.util.List;

/**
 * Request DTO used to register a new API in the registry.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRegistrationRequest {
    private String name;
    private String owner;
    private String description;
    private String detailedDescription;
    private String basePath;
    private String version;
    private String status;
    private String openApiSpec;
    private String environment;
    private List<String> tags;
    private List<String> domains;

}
