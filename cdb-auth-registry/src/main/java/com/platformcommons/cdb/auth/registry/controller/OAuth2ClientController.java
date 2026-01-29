package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.model.OAuth2Client;
import com.platformcommons.cdb.auth.registry.repository.OAuth2ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/oauth2/clients")
public class OAuth2ClientController {

    private final OAuth2ClientRepository clientRepository;
    private final SecureRandom random = new SecureRandom();

    public OAuth2ClientController(OAuth2ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @PostMapping
    public ResponseEntity<OAuth2Client> createClient(@RequestBody OAuth2Client client) {
        if (client.getClientId() == null) {
            client.setClientId(generateClientId());
        }
        if (client.getClientSecret() == null) {
            client.setClientSecret(generateClientSecret());
        }
        if (client.getGrantTypes() == null) {
            client.setGrantTypes(Set.of("authorization_code"));
        }
        if (client.getScopes() == null) {
            client.setScopes(Set.of("read", "write"));
        }
        
        OAuth2Client saved = clientRepository.save(client);
        return ResponseEntity.created(URI.create("/api/v1/oauth2/clients/" + saved.getId())).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<OAuth2Client>> listClients() {
        return ResponseEntity.ok(clientRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OAuth2Client> getClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OAuth2Client> updateClient(@PathVariable Long id, @RequestBody OAuth2Client client) {
        return clientRepository.findById(id)
                .map(existing -> {
                    existing.setClientName(client.getClientName());
                    existing.setDescription(client.getDescription());
                    existing.setRedirectUris(client.getRedirectUris());
                    existing.setScopes(client.getScopes());
                    existing.setRequireConsent(client.isRequireConsent());
                    existing.setLogoUrl(client.getLogoUrl());
                    return ResponseEntity.ok(clientRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        if (clientRepository.existsById(id)) {
            clientRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private String generateClientId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return "cdb_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateClientSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}