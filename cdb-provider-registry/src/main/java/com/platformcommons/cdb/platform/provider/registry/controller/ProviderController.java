package com.platformcommons.cdb.platform.provider.registry.controller;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderRegistrationRequest;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderResponse;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderSearchCriteria;
import com.platformcommons.cdb.platform.provider.registry.dto.SimpleProviderRequest;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderDiscoveryService;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderRegistrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controller exposing endpoints to manage providers: register, get, list.
 * <p>
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private static final Logger log = LoggerFactory.getLogger(ProviderController.class);

    private final ProviderRegistrationService registrationService;
    private final ProviderDiscoveryService providerDiscoveryService;

    public ProviderController(ProviderRegistrationService registrationService, ProviderDiscoveryService providerDiscoveryService) {
        this.registrationService = registrationService;
        this.providerDiscoveryService = providerDiscoveryService;
    }

    /**
     * Pre-check for step 2: validate provider code and admin email, trigger OTP and return key.
     */
    @PostMapping("/register/precheck")
    public ResponseEntity<?> precheck(@RequestBody PrecheckRequest req) {
        try {
            String key = registrationService.precheckAndInitiateOtp(req.code(), req.adminEmail());
            return ResponseEntity.ok(key);
        } catch (IllegalStateException ise) {
            log.error("Precheck conflict for code={} email={}: {}", req.code(), req.adminEmail(), ise.getMessage(), ise);
            return ResponseEntity.status(409).body(ise.getMessage());
        } catch (IllegalArgumentException iae) {
            log.error("Precheck bad request for code={} email={}: {}", req.code(), req.adminEmail(), iae.getMessage(), iae);
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (Exception ex) {
            log.error("Precheck failed for code={} email={}", req.code(), req.adminEmail(), ex);
            return ResponseEntity.internalServerError().body("Failed to initiate OTP");
        }
    }

    /**
     * Registers a new provider; returns 201 with location or 409 if code exists.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody ProviderRegistrationRequest request) {
        return registrationService.register(request)
                .<ResponseEntity<?>>map(resp -> ResponseEntity.created(URI.create("/api/v1/providers/" + resp.getId())).body(resp))
                .orElseGet(() -> ResponseEntity.status(409).body("Provider code already exists"));
    }

    /**
     * Gets a provider by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getById(@PathVariable Long id) {
        return registrationService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Search providers by query string or specific criteria.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProviderResponse>> search(
            @RequestParam(required = false,name = "q") String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag
    ) {
        try {
            ProviderSearchCriteria c = new ProviderSearchCriteria();
            c.setNameContains(name);
            c.setStatus(status);
            c.setTag(tag);
            return ResponseEntity.ok(providerDiscoveryService.search(c));
        } catch (Exception ex) {
            log.error("Search failed for name={}, status={}, tag={}", name, status, tag, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simplified provider registration for existing users.
     */
    @PostMapping
    public ResponseEntity<?> registerSimple(@RequestBody SimpleProviderRequest request) {
        try {
            ProviderResponse response = registrationService.registerProviderAndMapUser(request);
            return ResponseEntity.created(URI.create("/api/v1/providers/" + response.getId())).body(response);
        } catch (IllegalArgumentException iae) {
            log.error("Invalid request for simple registration: {}", iae.getMessage(), iae);
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (IllegalStateException ise) {
            log.error("Conflict in simple registration: {}", ise.getMessage(), ise);
            return ResponseEntity.status(409).body(ise.getMessage());
        } catch (Exception ex) {
            log.error("Failed to register provider", ex);
            return ResponseEntity.internalServerError().body("Failed to register provider");
        }
    }

    public record PrecheckRequest(String code, String adminEmail) {
    }
}
