package com.platformcommons.cdb.common.jpa;

import com.platformcommons.cdb.common.spring.SpringContextHolder;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.Objects;

/**
 * JPA entity listener that populates auditing fields for entities extending
 * {@link BaseEntity} using a pluggable {@link AuditContextProvider}.
 */
public class AuditableEntityListener {

    private AuditContextProvider getProvider() {
        return SpringContextHolder.getBean(AuditContextProvider.class);
    }

    @PrePersist
    public void setAuditOnCreate(Object entity) {
        if (!(entity instanceof BaseEntity base)) {
            return;
        }
        AuditContextProvider provider = getProvider();
        if (provider == null) {
            return; // No security context available (e.g., background jobs)
        }
        Long userId = safe(provider.getCurrentUserId());
        Long providerId = safe(provider.getCurrentProviderId());
        if (base.getCreatedBy() == null) {
            base.setCreatedBy(userId);
        }
        if (base.getCreatedByProvider() == null) {
            base.setCreatedByProvider(providerId);
        }
        base.setUpdatedBy(userId);
        base.setUpdatedByProvider(providerId);
    }

    @PreUpdate
    public void setAuditOnUpdate(Object entity) {
        if (!(entity instanceof BaseEntity base)) {
            return;
        }
        AuditContextProvider provider = getProvider();
        if (provider == null) {
            return;
        }
        Long userId = safe(provider.getCurrentUserId());
        Long providerId = safe(provider.getCurrentProviderId());
        base.setUpdatedBy(userId);
        base.setUpdatedByProvider(providerId);
    }

    private Long safe(Long value) {
        return Objects.requireNonNullElse(value, 0L);
    }
}
