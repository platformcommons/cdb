package com.platformcommons.cdb.platform.master.data.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain event representing a change in MasterData.
 */
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MasterDataChangeEvent {
    private String id; // master data id
    private String action; // CREATE, UPDATE, DELETE
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
