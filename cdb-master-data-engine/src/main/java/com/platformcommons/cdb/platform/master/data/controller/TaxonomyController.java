package com.platformcommons.cdb.platform.master.data.controller;

import com.platformcommons.cdb.platform.master.data.dto.TaxonomyResponse;
import com.platformcommons.cdb.platform.master.data.model.Taxonomy;
import com.platformcommons.cdb.platform.master.data.service.HierarchicalDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing endpoints to work with hierarchical taxonomies.
 */
@RestController
@RequestMapping("/api/mdm/taxonomies")
public class TaxonomyController {

    private final HierarchicalDataService hierarchicalDataService;

    public TaxonomyController(HierarchicalDataService hierarchicalDataService) {
        this.hierarchicalDataService = hierarchicalDataService;
    }

    /**
     * Returns the full taxonomy tree for a given domain.
     */
    @GetMapping("/{domain}")
    public ResponseEntity<TaxonomyResponse> getTree(@PathVariable String domain) {
        List<Taxonomy> nodes = hierarchicalDataService.loadTaxonomy(domain);
        return ResponseEntity.ok(TaxonomyResponse.of(domain, nodes));
    }
}
