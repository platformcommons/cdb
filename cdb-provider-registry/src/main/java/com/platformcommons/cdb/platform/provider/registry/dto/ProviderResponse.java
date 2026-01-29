package com.platformcommons.cdb.platform.provider.registry.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO representing Provider details sent back to clients.
 *
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String status;
    private String contactEmail;
    private String contactPhone;
    private String tags;
    private Instant createdAt;
    private Instant updatedAt;
}
