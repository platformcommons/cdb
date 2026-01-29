package com.platformcommons.cdb.platform.master.data.repository;

import com.platformcommons.cdb.platform.master.data.model.MasterData;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for MasterData persistence. Technology-agnostic stub.
 */
@Repository
public interface MasterDataRepository {
    MasterData save(MasterData data);
    Optional<MasterData> findById(String id);
    List<MasterData> findAll();
    List<MasterData> findByType(String type);
    void deleteById(String id);
}
