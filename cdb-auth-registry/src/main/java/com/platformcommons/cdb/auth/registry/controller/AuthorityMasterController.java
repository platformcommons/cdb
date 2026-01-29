package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.model.AuthorityMaster;
import com.platformcommons.cdb.auth.registry.repository.AuthorityMasterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/authority-masters")
public class AuthorityMasterController {

    private final AuthorityMasterRepository repository;

    public AuthorityMasterController(AuthorityMasterRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<AuthorityMaster> create(@RequestBody AuthorityMaster authority) {
        AuthorityMaster saved = repository.save(authority);
        return ResponseEntity.created(URI.create("/api/v1/authority-masters/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorityMaster> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AuthorityMaster>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<AuthorityMaster> findByCode(@PathVariable String code) {
        return repository.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorityMaster> update(@PathVariable Long id, @RequestBody AuthorityMaster input) {
        AuthorityMaster existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Authority not found"));
        existing.setName(input.getName());
        existing.setCode(input.getCode());
        existing.setProcessArea(input.getProcessArea());
        AuthorityMaster saved = repository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}