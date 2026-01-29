package com.platformcommons.cdb.platform.provider.registry.controller;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderEnvironmentDto;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderEnvironmentRequest;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderEnvironmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider-environments")
public class ProviderEnvironmentController {
    private final ProviderEnvironmentService service;

    public ProviderEnvironmentController(ProviderEnvironmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProviderEnvironmentDto> create(@RequestBody ProviderEnvironmentRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderEnvironmentDto> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ProviderEnvironmentDto>> getByProviderId(@PathVariable Long providerId) {
        return ResponseEntity.ok(service.findByProviderId(providerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderEnvironmentDto> update(@PathVariable Long id, @RequestBody ProviderEnvironmentRequest request) {
        return service.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}


