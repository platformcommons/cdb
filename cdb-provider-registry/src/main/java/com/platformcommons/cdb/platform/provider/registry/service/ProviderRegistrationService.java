package com.platformcommons.cdb.platform.provider.registry.service;

import com.platformcommons.cdb.platform.provider.registry.client.AuthRegistryClient;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderRegistrationRequest;
import com.platformcommons.cdb.platform.provider.registry.dto.ProviderResponse;
import com.platformcommons.cdb.platform.provider.registry.dto.SimpleProviderRequest;
import com.platformcommons.cdb.platform.provider.registry.mapper.ProviderMapper;
import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderMetadata;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderRepository;
import com.platformcommons.cdb.security.auth.CDBSecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service responsible for provider registration and basic lifecycle operations.
 * <p>
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.1.0
 * Since: 2025-09-15
 */
@Service
public class ProviderRegistrationService {

    private final ProviderRepository providerRepository;
    private final AuthRegistryClient authRegistryClient;
    private final ProviderMapper providerMapper;
    private final CDBSecurityUtil cdbSecurityUtil;
    public ProviderRegistrationService(ProviderRepository providerRepository,
                                       AuthRegistryClient authRegistryClient,
                                       ProviderMapper providerMapper, CDBSecurityUtil cdbSecurityUtil) {
        this.providerRepository = providerRepository;
        this.authRegistryClient = authRegistryClient;
        this.providerMapper = providerMapper;
        this.cdbSecurityUtil = cdbSecurityUtil;
    }

    /**
     * Pre-checks provider code and admin email and initiates OTP in auth-registry.
     *
     * @return OTP key if successful
     */
    @Transactional(readOnly = true)
    public String precheckAndInitiateOtp(String code, String adminEmail) {
        validateRequest(code, adminEmail);
        String key = authRegistryClient.initiateOtp(adminEmail);
        if (key == null) throw new IllegalStateException("Failed to initiate OTP");
        return key;
    }

    private void validateRequest(String code, String adminEmail) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (adminEmail == null || adminEmail.isBlank()) throw new IllegalArgumentException("adminEmail is required");
        if (providerRepository.findByCode(code).isPresent()) {
            throw new IllegalStateException("Provider code already exists");
        }
        if (authRegistryClient.userExists(adminEmail)) {
            throw new IllegalStateException("User with this email already exists");
        }
    }

    /**
     * Registers a new provider using request data following Option A:
     * 1) Persist provider first without owner (status=PENDING)
     * 2) Call Auth Registry to register admin user (composite placeholder)
     * 3) On success, update provider.ownerUserId and set status=ACTIVE
     * 4) On failure, mark provider FAILED and mutate unique/details with UUID suffix so the original code can be retried
     */
    @Transactional
    public Optional<ProviderResponse> register(ProviderRegistrationRequest req) {
        Optional<Provider> existing = providerRepository.findByCode(req.getCode());
        if (existing.isPresent()) {
            throw new IllegalStateException("Provider code already exists");
        }
        if (req.getAdminUsername() == null || req.getAdminUsername().isBlank()) {
            throw new IllegalArgumentException("adminUsername must be not be null or blank");
        }

        // 1) Save provider first without owner
        Provider provider = addNewProvider(req);
        try {
            // 2) Call Auth Registry to register admin user
            AuthRegistryClient.UserResponse user = addNewUserAndProviderMap(req, provider);
            // 3) Update provider with owner and activate
            Provider updated = updateProviderWithOwnerUserId(provider, user);
            return Optional.of(providerMapper.toResponse(updated));
        } catch (Exception ex) {
            // 4) Failure: mark FAILED and mutate fields so original code can be reused
            resetFailedProvider(provider);
            throw new IllegalStateException("Admin user registration failed; provider marked FAILED and released original code for retry", ex);
        }
    }

    private Provider updateProviderWithOwnerUserId(Provider provider, AuthRegistryClient.UserResponse user) {
        provider.setOwnerUserId(user.id());
        provider.setStatus("ACTIVE");
        return providerRepository.save(provider);
    }

    private void resetFailedProvider(Provider provider) {
        java.util.UUID uuid = java.util.UUID.randomUUID();
        String suffix = "-" + uuid;
        provider.setStatus("FAILED");
        if (provider.getCode() != null) provider.setCode(provider.getCode() + suffix);
        if (provider.getName() != null) provider.setName(provider.getName() + suffix);
        if (provider.getDescription() != null) provider.setDescription(provider.getDescription() + suffix);
        if (provider.getMetadata() != null) {
            ProviderMetadata meta = provider.getMetadata();
            if (meta.getContactEmail() != null) meta.setContactEmail(meta.getContactEmail() + suffix);
            if (meta.getContactPhone() != null) meta.setContactPhone(meta.getContactPhone() + suffix);
        }
        providerRepository.save(provider);
    }

    private AuthRegistryClient.UserResponse addNewUserAndProviderMap(ProviderRegistrationRequest req,
                                                                     Provider provider) {
        AuthRegistryClient.UserRegistrationRequest userReq =
                new AuthRegistryClient.UserRegistrationRequest(
                        req.getAdminUsername(), req.getAdminPassword(),
                        req.getAdminFirstName(), req.getAdminLastName(),
                        req.getOtpKey(),
                        req.getOtp(),
                        provider.getId(), provider.getCode()
                );
        AuthRegistryClient.UserResponse user = authRegistryClient.registerUser(userReq);
        if (user == null || user.id() == null) {
            throw new IllegalStateException("Failed to create admin user in Auth Registry");
        }
        return user;
    }

    private Provider addNewProvider(ProviderRegistrationRequest req) {
        Provider provider = new Provider();
        provider.setName(req.getName());
        provider.setCode(req.getCode());
        provider.setDescription(req.getDescription());
        provider.setStatus("PENDING");
        ProviderMetadata md = new ProviderMetadata();
        md.setContactEmail(req.getContactEmail());
        md.setContactPhone(req.getContactPhone());
        md.setTags(req.getTags());
        provider.setMetadata(md);
        provider.setOwnerUserId(null);
        provider = providerRepository.save(provider);
        return provider;
    }

    /**
     * Retrieves a provider by id and converts to response DTO.
     */
    @Transactional(readOnly = true)
    public Optional<ProviderResponse> getById(Long id) {
        return providerRepository.findById(id).map(providerMapper::toResponse);
    }

    /**
     * Search providers by name or code.
     */
    @Transactional(readOnly = true)
    public java.util.List<ProviderResponse> searchProviders(String query) {
        return providerRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query, query)
                .stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .map(providerMapper::toResponse)
                .toList();
    }

    /**
     * Simplified provider registration for existing users - creates provider and maps user to it.
     */
    @Transactional
    public ProviderResponse registerProviderAndMapUser(SimpleProviderRequest req) {
        // Validate request
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Provider name is required");
        }
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new IllegalArgumentException("Provider code is required");
        }
        
        // Check if provider code already exists
        if (providerRepository.findByCode(req.getCode()).isPresent()) {
            throw new IllegalStateException("Provider code already exists");
        }

        // Create provider
        Provider provider = new Provider();
        provider.setName(req.getName());
        provider.setCode(req.getCode());
        provider.setDescription(req.getDescription());
        provider.setStatus("ACTIVE");
        provider.setOwnerUserId(cdbSecurityUtil.getCurrentUserId());
        
        ProviderMetadata md = new ProviderMetadata();
        md.setContactEmail(req.getContactEmail());
        md.setContactPhone(req.getContactPhone());
        provider.setMetadata(md);
        
        provider = providerRepository.save(provider);
        authRegistryClient.mapUserToProvider(cdbSecurityUtil.getCurrentUserId(),
                provider.getId(), provider.getCode(), "ADMIN",cdbSecurityUtil.getCurrentAccessToken());

        return providerMapper.toResponse(provider);
    }
}
