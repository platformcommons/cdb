package com.platformcommons.cdb.platform.api.registry.repository;

import com.platformcommons.cdb.platform.api.registry.model.ApiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiAuditLogRepository extends JpaRepository<ApiAuditLog, Long> {
    List<ApiAuditLog> findByApiId(Long apiId);
}
