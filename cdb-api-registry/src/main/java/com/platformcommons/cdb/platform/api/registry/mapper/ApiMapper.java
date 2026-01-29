package com.platformcommons.cdb.platform.api.registry.mapper;

import com.platformcommons.cdb.platform.api.registry.dto.ApiRegistrationRequest;
import com.platformcommons.cdb.platform.api.registry.model.Api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Mapper utilities for converting between DTOs and Api domain model.
 */
public class ApiMapper {

    public static Api toNewEntity(ApiRegistrationRequest request) {
        String initialVersion = (request.getVersion() == null || request.getVersion().isBlank()) ? "1.0.0" : request.getVersion();
        return Api.builder()
                .id(0L)
                .name(request.getName())
                .owner(request.getOwner())
                .description(request.getDescription())
                .detailedDescription(request.getDetailedDescription())
                .basePath(request.getBasePath())
                .version(initialVersion)
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .openApiSpec(request.getOpenApiSpec())
                .tags(Optional.ofNullable(request.getTags()).orElseGet(List::of))
                .domains(Optional.ofNullable(request.getDomains()).orElseGet(List::of))
                .build();
    }

    public static Api applyUpdate(Api existing, ApiRegistrationRequest request, String newVersion) {
        // Update fields on the existing entity to preserve ID and BaseEntity audit fields
        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getOwner() != null) {
            existing.setOwner(request.getOwner());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getDetailedDescription() != null) {
            existing.setDetailedDescription(request.getDetailedDescription());
        }
        if (request.getBasePath() != null) {
            existing.setBasePath(request.getBasePath());
        }
        if (request.getOpenApiSpec() != null) {
            existing.setOpenApiSpec(request.getOpenApiSpec());
        }
        if (request.getTags() != null) {
            // If tags is provided as empty, we intentionally clear existing tags
            existing.setTags(request.getTags());
        }

        if (request.getDomains() != null) {
            // If domains is provided as empty, we intentionally clear existing tags
            existing.setDomains(request.getDomains());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (newVersion != null && !newVersion.isBlank()) {
            existing.setVersion(newVersion);
        }
        return existing;
    }
}
