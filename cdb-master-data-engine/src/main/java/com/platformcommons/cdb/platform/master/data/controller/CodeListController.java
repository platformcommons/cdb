package com.platformcommons.cdb.platform.master.data.controller;

import com.platformcommons.cdb.platform.master.data.model.CodeList;
import com.platformcommons.cdb.platform.master.data.service.MasterDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing code lists (enumerations used in master data).
 */
@RestController
@RequestMapping("/api/mdm/codelists")
public class CodeListController {

    private final MasterDataService masterDataService;

    public CodeListController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    /**
     * Returns all code lists of a given category.
     */
    @GetMapping
    public ResponseEntity<List<CodeList>> byCategory(@RequestParam String category) {
        return ResponseEntity.ok(masterDataService.findCodeLists(category));
    }
}
