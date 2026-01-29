package com.platformcommons.cdb.platform.provider.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "provider_configuration")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_code", nullable = false)
    private String configCode;

    @Column(name = "config_label", nullable = false)
    private String configLabel;

    @Column(name = "config_value")
    private String configValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigStatus status = ConfigStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigVisibility visibility = ConfigVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_data_type", nullable = false)
    private ConfigDataType configDataType = ConfigDataType.STRING;

    @Column(name = "has_list", nullable = false)
    private Boolean hasList = false;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @OneToMany(mappedBy = "providerConfiguration", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("configValueSequence")
    private List<ProviderConfigData> configDataList;
}