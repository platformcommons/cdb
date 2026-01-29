package com.platformcommons.cdb.platform.master.data.service;

import com.platformcommons.cdb.platform.master.data.dto.MasterDataRequest;
import com.platformcommons.cdb.platform.master.data.model.CodeList;
import com.platformcommons.cdb.platform.master.data.model.MasterData;
import com.platformcommons.cdb.platform.master.data.repository.MasterDataRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service encapsulating operations on MasterData and related lookups.
 */
@Service
public class MasterDataService {

    private final MasterDataRepository repository;

    public MasterDataService(MasterDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates or updates a MasterData entity. Placeholder persists in-memory via repository stub.
     */
    public MasterData upsert(MasterDataRequest request) {
        MasterData md = new MasterData();
        md.setId(request.getId() != null ? request.getId() : UUID.randomUUID().toString());
        md.setType(request.getType());
        md.setAttributes(request.getAttributes());
        md.setUpdatedAt(Instant.now());
        if (md.getCreatedAt() == null) {
            md.setCreatedAt(Instant.now());
        }
        return repository.save(md);
    }

    public Optional<MasterData> findById(String id) {
        return repository.findById(id);
    }

    public List<MasterData> list(String type) {
        if (type == null || type.isBlank()) {
            return repository.findAll();
        }
        return repository.findByType(type);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    /**
     * Returns code lists under a given category. Placeholder implementation returns empty list.
     */
    public List<CodeList> findCodeLists(String category) {
        return new ArrayList<>();
    }
}
