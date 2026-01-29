package com.platformcommons.cdb.platform.api.registry.controller;

import com.platformcommons.cdb.platform.api.registry.dto.ApiDetailResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiListResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiRegistrationRequest;
import com.platformcommons.cdb.platform.api.registry.dto.ApiResponse;
import com.platformcommons.cdb.platform.api.registry.service.ApiManagementService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for API management operations.
 */
@RestController
@RequestMapping("/api/v1/registry/apis")
public class ApiController {

    private final ApiManagementService managementService;

    public ApiController(ApiManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getApis(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiListResponse response = managementService.getApis(PageRequest.of(page, size),null);
        return ResponseEntity.ok(ApiResponse.success("APIs fetched", response));
    }

    @GetMapping("/{apiId}")
    public ResponseEntity<ApiDetailResponse> getApiDetail(@PathVariable Long apiId) {
        return ResponseEntity.ok(managementService.getApiDetail(apiId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> registerApi(@RequestBody ApiRegistrationRequest request) {
        Long id = managementService.registerApi(request);
        return ResponseEntity.ok(ApiResponse.success("API registered", id));
    }

    @PutMapping("/{apiId}")
    public ResponseEntity<ApiResponse> updateApi(@PathVariable Long apiId, @RequestBody ApiRegistrationRequest request) {
        managementService.updateApi(apiId, request);
        return ResponseEntity.ok(ApiResponse.success("API updated", apiId));
    }

    @GetMapping("/tags/search")
    public ResponseEntity<java.util.List<String>> searchTags(@RequestParam String query) {
        return ResponseEntity.ok(managementService.searchTags(query));
    }

    @GetMapping("/domains/search")
    public ResponseEntity<java.util.List<String>> searchDomains(@RequestParam String query) {
        return ResponseEntity.ok(managementService.searchDomains(query));
    }

}
