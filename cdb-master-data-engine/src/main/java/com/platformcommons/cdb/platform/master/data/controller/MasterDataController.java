package com.platformcommons.cdb.platform.master.data.controller;

import com.platformcommons.cdb.platform.master.data.dto.MasterDataRequest;
import com.platformcommons.cdb.platform.master.data.dto.SynchronizationStatus;
import com.platformcommons.cdb.platform.master.data.model.MasterData;
import com.platformcommons.cdb.platform.master.data.service.MasterDataService;
import com.platformcommons.cdb.platform.master.data.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD endpoints for Master Data entities.
 * <p>
 * This controller delegates business logic to {@link MasterDataService} and validation to
 * {@link ValidationService}. It provides minimal placeholder implementations to be
 * extended with persistence and domain rules later.
 * </p>
 */
@RestController
@RequestMapping("/api/mdm/master-data")
public class MasterDataController {

    private final MasterDataService masterDataService;
    private final ValidationService validationService;

    public MasterDataController(MasterDataService masterDataService, ValidationService validationService) {
        this.masterDataService = masterDataService;
        this.validationService = validationService;
    }

    /**
     * Creates or updates a MasterData entity.
     */
    @PostMapping
    public ResponseEntity<MasterData> upsert(@RequestBody MasterDataRequest request) {
        validationService.validate(request);
        MasterData saved = masterDataService.upsert(request);
        return ResponseEntity.ok(saved);
    }

    /**
     * Retrieves a MasterData entity by its identifier.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MasterData> getById(@PathVariable String id) {
        return ResponseEntity.of(masterDataService.findById(id));
    }

    /**
     * Lists MasterData records by type (optional).
     */
    @GetMapping
    public ResponseEntity<List<MasterData>> list(@RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(masterDataService.list(type));
    }

    /**
     * Deletes a MasterData record.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<SynchronizationStatus> delete(@PathVariable String id) {
        masterDataService.delete(id);
        return ResponseEntity.ok(SynchronizationStatus.ok("Deleted", id));
    }
}
