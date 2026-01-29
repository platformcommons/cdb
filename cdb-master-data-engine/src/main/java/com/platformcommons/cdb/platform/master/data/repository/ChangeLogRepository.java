package com.platformcommons.cdb.platform.master.data.repository;

import com.platformcommons.cdb.platform.master.data.model.ChangeLog;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for change logs associated with master data modifications.
 */
@Repository
public interface ChangeLogRepository {
    ChangeLog save(ChangeLog log);
    List<ChangeLog> findByEntityId(String entityId);
}
