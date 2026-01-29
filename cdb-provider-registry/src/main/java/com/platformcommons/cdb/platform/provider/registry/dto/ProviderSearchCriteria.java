package com.platformcommons.cdb.platform.provider.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO representing criteria for searching providers.
 * All fields are optional; when provided they filter the results.
 *
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProviderSearchCriteria {
    private String nameContains;
    private String status;
    private String tag;
}
