package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;
import com.platformcommons.cdb.auth.registry.service.UserProviderMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user-provider-mappings")
public class UserProviderMappingController {

    private final UserProviderMappingService service;

    public UserProviderMappingController(UserProviderMappingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserProviderMapping> create(@RequestBody UserProviderMapping mapping) {
        UserProviderMapping saved = service.create(mapping);
        return ResponseEntity.created(URI.create("/api/v1/user-provider-mappings/" + saved.getId())).body(saved);
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<UserProviderMapping> updateStatus(@PathVariable Long id, @RequestParam UserProviderMapping.MappingStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = service.delete(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}