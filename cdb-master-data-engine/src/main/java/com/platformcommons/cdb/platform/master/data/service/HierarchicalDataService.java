package com.platformcommons.cdb.platform.master.data.service;

import com.platformcommons.cdb.platform.master.data.model.Taxonomy;
import com.platformcommons.cdb.platform.master.data.repository.TaxonomyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for hierarchical data operations such as taxonomies and trees.
 */
@Service
public class HierarchicalDataService {

    private final TaxonomyRepository taxonomyRepository;

    public HierarchicalDataService(TaxonomyRepository taxonomyRepository) {
        this.taxonomyRepository = taxonomyRepository;
    }

    /**
     * Loads taxonomy nodes for a given domain. Placeholder returns empty list.
     */
    public List<Taxonomy> loadTaxonomy(String domain) {
        return new ArrayList<>();
    }
}
