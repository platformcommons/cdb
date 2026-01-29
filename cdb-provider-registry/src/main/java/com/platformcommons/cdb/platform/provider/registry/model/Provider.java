package com.platformcommons.cdb.platform.provider.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Provider represents a service provider registered in the CDB ecosystem.
 * <p>
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@Entity
@Table(name = "cdb_providers")
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Provider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human readable provider name
     */
    @Column(nullable = false)
    private String name;

    /**
     * Globally unique provider code/slug
     */
    @Column(unique = true, nullable = false, updatable = false)
    private String code;

    /**
     * Optional textual description
     */
    @Column(length = 2048)
    private String description;

    /**
     * Current lifecycle status e.g., ACTIVE, INACTIVE, PENDING
     */
    @Column(nullable = false)
    private String status = "PENDING";

    /**
     * ID of the owner user (from Auth Registry) mapped to this provider
     */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /**
     * Embedded metadata fields
     */
    @Embedded
    private ProviderMetadata metadata;
}
