package com.platformcommons.cdb.platform.provider.registry.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_config_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfigData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "config_value_sequence", nullable = false)
    private Integer configValueSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_configuration_id")
    private ProviderConfiguration providerConfiguration;
}