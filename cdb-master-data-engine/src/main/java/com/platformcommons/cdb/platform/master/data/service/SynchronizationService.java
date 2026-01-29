package com.platformcommons.cdb.platform.master.data.service;

import com.platformcommons.cdb.platform.master.data.dto.SynchronizationStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service responsible for synchronizing master data with upstream/downstream systems.
 */
@Service
public class SynchronizationService {

    /**
     * Triggers a synchronization job. Placeholder returns a generated job id and OK status.
     */
    public SynchronizationStatus triggerFullSync() {
        return SynchronizationStatus.ok("Full sync triggered", UUID.randomUUID().toString());
    }

    /**
     * Returns the status of a synchronization job. Placeholder returns a static response.
     */
    public SynchronizationStatus getStatus(String jobId) {
        SynchronizationStatus status = new SynchronizationStatus();
        status.setJobId(jobId);
        status.setStatus("COMPLETED");
        status.setMessage("Placeholder status");
        status.setLastUpdated(Instant.now());
        return status;
    }
}
