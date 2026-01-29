package com.platformcommons.cdb.platform.provider.registry.controller;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigurationDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderConfigurationRequest;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderConfigurationService;
import com.platformcommons.cdb.security.auth.CDBContextAuthentication;
import com.platformcommons.cdb.security.auth.CDBSecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/provider-configurations")
public class ProviderConfigurationController {

    private final ProviderConfigurationService service;
    private final CDBSecurityUtil cdbSecurityUtil;
    public ProviderConfigurationController(ProviderConfigurationService service, CDBSecurityUtil cdbSecurityUtil) {
        this.service = service;
        this.cdbSecurityUtil = cdbSecurityUtil;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ProviderConfigurationRequest request) {
        try {
            Long providerId = cdbSecurityUtil.getCurrentProviderId();
            ProviderConfigurationDto created = service.create(request, providerId);
            return ResponseEntity.created(URI.create("/api/v1/provider-configurations/" + created.getId()))
                    .body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderConfigurationDto> findById(
            @PathVariable Long id) {
        Long providerId = cdbSecurityUtil.getCurrentProviderId();
        return service.findById(id, providerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProviderConfigurationDto>> findAll() {
        Long providerId = cdbSecurityUtil.getCurrentProviderId();
        List<ProviderConfigurationDto> configs = service.findByProviderId(providerId);
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderConfigurationRequest request) {
        try {
            Long providerId = cdbSecurityUtil.getCurrentProviderId();
            return service.update(id, request, providerId)
                    .map(config -> ResponseEntity.ok((Object) config))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        Long providerId = cdbSecurityUtil.getCurrentProviderId();
        boolean deleted = service.delete(id, providerId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/public")
    public ResponseEntity<ProviderConfigurationDto> getPublicConfig(
            @RequestParam String configCode,
            @RequestParam String providerCode) {
        return service.findPublicConfig(configCode, providerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}