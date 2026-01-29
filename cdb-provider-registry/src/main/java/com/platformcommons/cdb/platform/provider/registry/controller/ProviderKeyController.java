package com.platformcommons.cdb.platform.provider.registry.controller;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderKeyDtos;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderKeyService;
import com.platformcommons.cdb.security.auth.CDBSecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers/{providerId}/keys")
public class ProviderKeyController {

    private final ProviderKeyService providerKeyService;
    private final CDBSecurityUtil cdbSecurityUtil;

    public ProviderKeyController(ProviderKeyService providerKeyService, CDBSecurityUtil cdbSecurityUtil) {
        this.providerKeyService = providerKeyService;
        this.cdbSecurityUtil = cdbSecurityUtil;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@PathVariable Long providerId,
                                      @Valid @RequestBody ProviderKeyDtos.GenerateKeyRequest request) {
        providerId = cdbSecurityUtil.getCurrentProviderId();
        if (request.getClientId() != null && request.getClientId().length() > 100) {
            return ResponseEntity.badRequest().body("Client ID cannot exceed 100 characters");
        }
        
        return providerKeyService.generateKey(providerId, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/symmetric/request")
    public ResponseEntity<?> requestSymmetricKey(@PathVariable Long providerId,
                                      @Valid @RequestBody ProviderKeyDtos.GenerateKeyRequest request) {
        if (request.getClientId() != null && request.getClientId().length() > 100) {
            return ResponseEntity.badRequest().body("Client ID cannot exceed 100 characters");
        }

        return providerKeyService.generateSymmetricKey(providerId, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProviderKeyDtos.KeySummary>> list(@PathVariable Long providerId) {
        providerId = cdbSecurityUtil.getCurrentProviderId();
        return ResponseEntity.ok(providerKeyService.listKeys(providerId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ProviderKeyDtos.KeySummary>> listPublic(@PathVariable Long providerId) {
        return ResponseEntity.ok(providerKeyService.listKeysPublic(providerId));
    }


    @PutMapping("/{keyId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long providerId, @PathVariable String keyId) {
        providerId = cdbSecurityUtil.getCurrentProviderId();
        boolean success = providerKeyService.deactivateKey(providerId, keyId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

}
