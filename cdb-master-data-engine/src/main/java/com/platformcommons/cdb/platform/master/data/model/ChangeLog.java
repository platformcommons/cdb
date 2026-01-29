package com.platformcommons.cdb.platform.master.data.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Captures changes applied to master data entities for audit/sync purposes.
 */
@Getter
@Setter
@ToString
@Builder
public class ChangeLog {
    private String id;
    private String entityId;
    private String entityType;
    private String action; // CREATE, UPDATE, DELETE
    private String changedBy;
    private Instant changedAt;
    private String details;
}
