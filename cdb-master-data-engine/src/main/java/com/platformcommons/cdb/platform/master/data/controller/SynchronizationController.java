package com.platformcommons.cdb.platform.master.data.controller;

import com.platformcommons.cdb.platform.master.data.dto.SynchronizationStatus;
import com.platformcommons.cdb.platform.master.data.service.SynchronizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing endpoints to trigger and monitor synchronization jobs.
 */
@RestController
@RequestMapping("/api/mdm/sync")
public class SynchronizationController {

    private final SynchronizationService synchronizationService;

    public SynchronizationController(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    /**
     * Triggers a full synchronization job.
     */
    @PostMapping("/full")
    public ResponseEntity<SynchronizationStatus> triggerFull() {
        return ResponseEntity.ok(synchronizationService.triggerFullSync());
    }

    /**
     * Retrieves status by job id.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<SynchronizationStatus> status(@PathVariable String jobId) {
        return ResponseEntity.ok(synchronizationService.getStatus(jobId));
    }
}
