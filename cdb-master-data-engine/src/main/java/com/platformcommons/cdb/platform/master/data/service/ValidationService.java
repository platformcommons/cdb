package com.platformcommons.cdb.platform.master.data.service;

import com.platformcommons.cdb.platform.master.data.dto.MasterDataRequest;
import com.platformcommons.cdb.platform.master.data.model.ValidationRule;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service for validating master data according to configured rules.
 */
@Service
public class ValidationService {

    /**
     * Validates a MasterDataRequest against rules. Placeholder performs minimal checks.
     * Throws IllegalArgumentException if basic required fields are missing.
     */
    public void validate(MasterDataRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
    }

    /**
     * Returns active validation rules. Placeholder returns an empty list.
     */
    public List<ValidationRule> getRules() {
        return Collections.emptyList();
    }
}
