package com.platformcommons.cdb.platform.provider.registry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "provider_environment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEnvironment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long providerId;

    @Enumerated(EnumType.STRING)
    private EnvironmentType environmentType;

    private String baseUrl;

    private String uptimeStatus;

    private Integer rateLimit;

    private String remarks;

    // Getters and setters
    // ...

    public enum EnvironmentType {
        PRODUCTION, SANDBOX
    }
}