package com.platformcommons.cdb.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * BaseEntity captures common auditing and lifecycle fields for all JPA entities.
 * Move shared fields here and extend this class from entity classes across modules.
 */
@MappedSuperclass
@EntityListeners(AuditableEntityListener.class)
@Getter
@Setter
@ToString
public abstract class BaseEntity {

    /** Creation timestamp */
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")

    private Instant createdAt;

    /** Updated timestamp */
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant updatedAt;

    /** User who created the record */
    @Column(name = "created_by", columnDefinition = "BIGINT DEFAULT 0")

    private Long createdBy;

    /** User who last updated the record */
    @Column(name = "updated_by", columnDefinition = "BIGINT DEFAULT 0")
    private Long updatedBy;

    /** User who created the record */
    @Column(name = "created_by_provider", columnDefinition = "BIGINT DEFAULT 0")
    private Long createdByProvider;

    /** User who last updated the record */
    @Column(name = "updated_by_provider", columnDefinition = "BIGINT DEFAULT 0")
    private Long updatedByProvider;

    /** Active flag for soft lifecycle control */
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT 1")

    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}
