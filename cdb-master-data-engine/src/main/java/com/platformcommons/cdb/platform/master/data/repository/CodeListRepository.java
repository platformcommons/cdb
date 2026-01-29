package com.platformcommons.cdb.platform.master.data.repository;

import com.platformcommons.cdb.platform.master.data.model.CodeList;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository abstraction for code lists.
 */
@Repository
public interface CodeListRepository {
    List<CodeList> findByCategory(String category);
}
