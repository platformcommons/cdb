package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.model.RoleMaster;
import com.platformcommons.cdb.auth.registry.service.RoleMasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/role-masters")
public class RoleMasterController {

    private final RoleMasterService roleService;

    public RoleMasterController(RoleMasterService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleMaster> create(@RequestBody RoleMaster role) {
        RoleMaster saved = roleService.create(role);
        return ResponseEntity.created(URI.create("/api/v1/role-masters/" + saved.getId())).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<RoleMaster>> list() {
        return ResponseEntity.ok(roleService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleMaster> get(@PathVariable Long id) {
        return roleService.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<RoleMaster> findByCode(@PathVariable String code) {
        return roleService.findByCode(code).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleMaster> update(@PathVariable Long id, @RequestBody RoleMaster input) {
        RoleMaster saved = roleService.update(id, input);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean existed = roleService.delete(id);
        return existed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/authorities")
    public ResponseEntity<RoleMaster> assignAuthoritiesByCodes(@PathVariable Long id, @RequestBody List<String> authorityCodes) {
        RoleMaster role = roleService.assignAuthoritiesByCodes(id, authorityCodes);
        return ResponseEntity.ok(role);
    }

    @DeleteMapping("/{id}/authorities")
    public ResponseEntity<RoleMaster> removeAuthoritiesByCodes(@PathVariable Long id, @RequestBody List<String> authorityCodes) {
        RoleMaster role = roleService.removeAuthoritiesByCodes(id, authorityCodes);
        return ResponseEntity.ok(role);
    }
}