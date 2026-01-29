package com.platformcommons.cdb.auth.registry.service.impl;

import com.platformcommons.cdb.auth.registry.dto.UserRegistrationRequest;
import com.platformcommons.cdb.auth.registry.model.RoleMaster;
import com.platformcommons.cdb.auth.registry.model.User;
import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;
import com.platformcommons.cdb.auth.registry.repository.UserRepository;
import com.platformcommons.cdb.auth.registry.service.UserManagementService;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Service
@Primary
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final com.platformcommons.cdb.auth.registry.service.OtpRegistryService otpRegistryService;
    private final com.platformcommons.cdb.auth.registry.repository.UserProviderMappingRepository userProviderMappingRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public UserManagementServiceImpl(UserRepository userRepository,
                                     com.platformcommons.cdb.auth.registry.service.OtpRegistryService otpRegistryService,
                                     com.platformcommons.cdb.auth.registry.repository.UserProviderMappingRepository userProviderMappingRepository) {
        this.userRepository = userRepository;
        this.otpRegistryService = otpRegistryService;
        this.userProviderMappingRepository = userProviderMappingRepository;
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        validateUser(request);
        User saved = addUserInternal(request);
        addUserProviderMapping(request, saved);
        return saved;
    }

    private void addUserProviderMapping(UserRegistrationRequest request, User saved) {
        if (request.getProviderCode() != null && !request.getProviderCode().isBlank()) {
            String pCode = normalize(request.getProviderCode());
            java.time.Instant now = java.time.Instant.now();
            userProviderMappingRepository
                    .findByUserIdAndProviderCodeAndStatus(saved.getId(), pCode,
                            UserProviderMapping.MappingStatus.ACTIVE)
                    .orElseGet(() -> {
                        UserProviderMapping mapping = UserProviderMapping.builder()
                                .userId(saved.getId())
                                .providerId(request.getProviderId())
                                .providerCode(pCode)
                                .mappedAt(now)
                                //  .roles(Set.of(adminRole())) //TODO check for duplicate exception
                                .status(UserProviderMapping.MappingStatus.ACTIVE)
                                .build();
                        return userProviderMappingRepository.save(mapping);
                    });
        }
    }

    private RoleMaster adminRole() {
        return RoleMaster.builder()
                .id(2L)
                .type("SYSTEM")
                .code("PROLE.PROVIDER_ADMIN").label("Provider Admin").build();
    }

    private void validateUser(UserRegistrationRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("username and password are required");
        }
        if (request.getOtpKey() == null || request.getOtpKey().isBlank()) {
            throw new IllegalArgumentException("OTPKey is required and must be validated before registration");
        }
        // Consume validated key to prevent reuse
        boolean isValid = otpRegistryService.verify(request.getOtpKey(), request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new IllegalStateException("OTP not validated for this email or key already used/expired");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already exists");
        }
    }

    private User addUserInternal(UserRegistrationRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(normalize(request.getEmail()))
                .passwordHash(encoder.encode(request.getPassword()))
                .enabled(true)
                .mfaEnabled(false)
                .lastLogin(null)
                .build();
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalize(email)).map(user -> User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .lastLogin(user.getLastLogin())
                .build());
    }


    @Override
    public User updateUser(Long userId, User user) {
        User existing = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            String uname = normalize(user.getUsername());
            if (!Objects.equals(uname, existing.getUsername()) &&
                    userRepository.findByEmail(uname).isPresent()) {
                throw new IllegalStateException("Username already exists");
            }
            existing.setUsername(uname);
        }
        return userRepository.save(existing);
    }

    @Override
    public void setUserEnabled(Long userId, boolean enabled) {
        User existing = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        existing.setEnabled(enabled);
        userRepository.save(existing);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}
