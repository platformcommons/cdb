package com.platformcommons.cdb.platform.api.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Request DTO for searching APIs based on various filters.
 */
@Getter
@Setter
@ToString
@Builder
public class ApiSearchRequest {
    private String query;
    private List<String> tags;
    private List<String> domains;
    private String owner;
    private String status;
}
