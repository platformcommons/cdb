package com.platformcommons.cdb.platform.master.data.repository;

import com.platformcommons.cdb.platform.master.data.model.Taxonomy;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository abstraction for taxonomy nodes.
 */
@Repository
public interface TaxonomyRepository {
    List<Taxonomy> findByDomain(String domain);
}
