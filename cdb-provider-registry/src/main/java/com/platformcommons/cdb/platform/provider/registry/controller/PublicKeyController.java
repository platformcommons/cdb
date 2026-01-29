package com.platformcommons.cdb.platform.provider.registry.controller;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderKeyDtos;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderKey;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-keys")
public class PublicKeyController {

    private final ProviderKeyService providerKeyService;

    public PublicKeyController(ProviderKeyService providerKeyService) {
        this.providerKeyService = providerKeyService;
    }

    @GetMapping("/{providerCode}/{keyType}")
    public ResponseEntity<ProviderKeyDtos.PublicKeyResponse> getPublicKey(
            @PathVariable String providerCode,
            @PathVariable ProviderKey.KeyType keyType) {

        return providerKeyService.getPublicKeyByProviderCodeAndKeyType(providerCode, keyType)
                .map(publicKey -> ResponseEntity.ok(new ProviderKeyDtos.PublicKeyResponse(publicKey)))
                .orElse(ResponseEntity.notFound().build());
    }


}