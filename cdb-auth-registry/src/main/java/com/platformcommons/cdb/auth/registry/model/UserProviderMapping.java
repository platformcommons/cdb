package com.platformcommons.cdb.auth.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_provider_mapping",
        indexes = {
                @Index(name = "idx_upm_user_provider", columnList = "user_id,provider_code,status")
        })
public class UserProviderMapping extends BaseEntity {

    public enum MappingStatus {ACTIVE, INACTIVE, SUSPENDED, REQUESTED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "provider_code", length = 100, nullable = false)
    private String providerCode;

    @Column(name = "mapped_at", nullable = false)
    private Instant mappedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MappingStatus status = MappingStatus.ACTIVE;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_provider_role_map",
            joinColumns = @JoinColumn(name = "user_provider_mapping_id"),
            inverseJoinColumns = @JoinColumn(name = "role_master_id"))
    @Builder.Default
    private Set<RoleMaster> roles = new HashSet<>();

}
