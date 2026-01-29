package com.platformcommons.cdb.common.jpa;

/**
 * Provides current audit context information (user and provider IDs).
 * Implementations should be provided by modules that integrate with the
 * security infrastructure (e.g., cdb-security-lib).
 */
public interface AuditContextProvider {
    Long getCurrentUserId();
    Long getCurrentProviderId();
}
