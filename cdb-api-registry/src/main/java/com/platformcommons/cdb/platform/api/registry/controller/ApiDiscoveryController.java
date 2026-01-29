package com.platformcommons.cdb.platform.api.registry.controller;

import com.platformcommons.cdb.platform.api.registry.dto.ApiDetailResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiListResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiSearchRequest;
import com.platformcommons.cdb.platform.api.registry.dto.DomainStatsResponse;
import com.platformcommons.cdb.platform.api.registry.model.Api;
import com.platformcommons.cdb.platform.api.registry.service.ApiDiscoveryService;
import com.platformcommons.cdb.platform.api.registry.service.ApiManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller exposing API discovery endpoints like search and listing.
 */
@RestController
@RequestMapping("/api/v1/api-registry/discovery")
public class ApiDiscoveryController {

    private final ApiDiscoveryService discoveryService;
    private final ApiManagementService managementService;

    public ApiDiscoveryController(ApiDiscoveryService discoveryService, ApiManagementService managementService) {
        this.discoveryService = discoveryService;
        this.managementService = managementService;
    }



    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchGet(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> domains,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        status = "PUBLISHED";
        ApiListResponse response = managementService.searchApis(query, tags, domains, owner, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Search completed", response));
    }

    /**
     * Lists APIs using slice-style pagination with optional filters and sort order.
     * Example: GET /apis?page=0&size=20&sort=updatedAt,desc&owner=team-a&createdByProvider=10
     */
    @GetMapping("/apis")
    public ResponseEntity<ApiResponse> listApis(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "createdByProvider", required = false) Long createdByProvider
    ) {
        Page<Api> slice = discoveryService.listApisSlice(page, size, sort, owner, createdByProvider);

        List<ApiListResponse.ApiSummary> summaries = slice.getContent().stream().map(api ->
                ApiListResponse.ApiSummary.builder()
                        .id(api.getId())
                        .name(api.getName())
                        .description(api.getDescription())
                        .version(api.getVersion())
                        .status(api.getStatus())
                        .owner(api.getOwner())
                        .updatedAt(api.getUpdatedAt())
                        .tags(api.getTags())
                        .domains(api.getDomains())
                        .build()
        ).collect(Collectors.toList());

        ApiListResponse response = ApiListResponse.builder()
                .apis(summaries)
                .totalElements(-1)
                .totalPages(-1)
                .currentPage(slice.getNumber())
                .pageSize(slice.getSize())
                .hasNext(slice.hasNext())
                .numberOfElements(slice.getNumberOfElements())
                .build();

        return ResponseEntity.ok(ApiResponse.success("APIs fetched", response));
    }

    /**
     * Fetch detailed information for a specific API card clicked in the marketplace.
     */
    @GetMapping("/apis/{apiId}")
    public ResponseEntity<ApiResponse> getApiDetail(@PathVariable("apiId") Long apiId) {
        ApiDetailResponse detail = managementService.getApiDetail(apiId);
        return ResponseEntity.ok(ApiResponse.success("API detail fetched", detail));
    }

    @GetMapping("/domains/stats")
    public ResponseEntity<DomainStatsResponse> getDomainStats() {
        return ResponseEntity.ok(managementService.getDomainStats());
    }

    @GetMapping("/domains/{domain}/apis")
    public ResponseEntity<ApiResponse> getApisByDomain(
            @PathVariable String domain,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiListResponse response = managementService.getApisByDomain(domain, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("APIs fetched for domain", response));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse> getAvailableTags(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> tags = discoveryService.getAvailableTags(search, limit);
        return ResponseEntity.ok(ApiResponse.success("Available tags fetched", Map.of("tags", tags)));
    }

    @GetMapping("/domains")
    public ResponseEntity<ApiResponse> getAvailableDomains(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> domains = discoveryService.getAvailableDomains(search, limit);
        return ResponseEntity.ok(ApiResponse.success("Available domains fetched", Map.of("domains", domains)));
    }

    @GetMapping("/owners")
    public ResponseEntity<ApiResponse> getAvailableOwners(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> owners = discoveryService.getAvailableOwners(search, limit);
        return ResponseEntity.ok(ApiResponse.success("Available owners fetched", Map.of("owners", owners)));
    }
}
