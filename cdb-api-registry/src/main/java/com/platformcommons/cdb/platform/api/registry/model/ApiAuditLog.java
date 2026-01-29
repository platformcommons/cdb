package com.platformcommons.cdb.platform.api.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Audit log for tracking API changes.
 */
@Entity
@Table(name = "cdb_api_audit_log")
@Getter
@Setter
@ToString
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ApiAuditLog  extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(length = 36)
    private Long id;

    @Column(name = "api_id", nullable = false, length = 128)
    private Long apiId;

    @Column(name = "version", length = 64)
    private String version;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "changed_by", length = 128)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Lob
    @Column(name = "old_values_json", columnDefinition = "LONGTEXT")
    private String oldValuesJson;

    @Lob
    @Column(name = "new_values_json", columnDefinition = "LONGTEXT")
    private String newValuesJson;

    @Column(name = "change_description", length = 1024)
    private String changeDescription;
}