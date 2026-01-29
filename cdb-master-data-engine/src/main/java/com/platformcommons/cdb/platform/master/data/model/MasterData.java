package com.platformcommons.cdb.platform.master.data.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a unit of master data (e.g., reference entity).
 */
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MasterData {
    private String id;
    private String type;
    private Map<String, Object> attributes;
    private Instant createdAt;
    private Instant updatedAt;
}
