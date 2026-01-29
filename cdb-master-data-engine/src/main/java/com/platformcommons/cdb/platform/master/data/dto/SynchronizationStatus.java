package com.platformcommons.cdb.platform.master.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Represents the status of a synchronization job.
 */
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class SynchronizationStatus {
    private String jobId;
    private String status; // QUEUED, RUNNING, COMPLETED, FAILED
    private String message;
    private Instant lastUpdated;

    public static SynchronizationStatus ok(String message, String jobId) {
        return SynchronizationStatus.builder()
                .status("OK")
                .message(message)
                .jobId(jobId)
                .lastUpdated(Instant.now())
                .build();
    }
}
