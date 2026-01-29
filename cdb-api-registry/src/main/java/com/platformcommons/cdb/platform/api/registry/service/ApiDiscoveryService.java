package com.platformcommons.cdb.platform.api.registry.service;

import com.platformcommons.cdb.platform.api.registry.dto.ApiSearchRequest;
import com.platformcommons.cdb.platform.api.registry.model.Api;
import com.platformcommons.cdb.platform.api.registry.repository.ApiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service providing discovery capabilities over registered APIs.
 */
@Service
public class ApiDiscoveryService {

    private final ApiRepository apiRepository;

    public ApiDiscoveryService(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    /**
     * Performs a search across APIs. Placeholder implementation returns an empty list.
     */
    public List<Api> search(ApiSearchRequest request) {
        return new ArrayList<>();
    }

    /**
     * Returns a slice of APIs filtered by optional owner and createdByProvider, with sorting.
     */
    public Page<Api> listApisSlice(Integer page, Integer size, String sort, String owner, Long createdByProvider) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 200) ? 20 : size;

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(p, s, sortObj);

        Specification<Api> spec = Specification.where(null);
        if (owner != null && !owner.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("owner"), owner));
        }
        if (createdByProvider != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("createdByProvider"), createdByProvider));
        }
        // Only active records and exclude DRAFT status
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isActive")));
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), "PUBLISHED"));

        return apiRepository.findAll(spec, pageable);
    }

    private Sort parseSort(String sort) {
        // default sort by updatedAt desc
        String property = "updatedAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) {
                property = parts[0].trim();
            }
            if (parts.length >= 2) {
                String dir = parts[1].trim().toLowerCase();
                if (Objects.equals(dir, "asc")) direction = Sort.Direction.ASC;
                if (Objects.equals(dir, "desc")) direction = Sort.Direction.DESC;
            }
        }
        return Sort.by(direction, property);
    }

    public List<String> getAvailableTags(String search, int limit) {
        if (search != null && !search.trim().isEmpty()) {
            return apiRepository.findDistinctTagsByActiveApisWithSearch(search.trim(), PageRequest.of(0, limit));
        }
        return apiRepository.findDistinctTagsByActiveApis(PageRequest.of(0, limit));
    }

    public List<String> getAvailableDomains(String search, int limit) {
        if (search != null && !search.trim().isEmpty()) {
            return apiRepository.findDistinctDomainsByActiveApisWithSearch(search.trim(), PageRequest.of(0, limit));
        }
        return apiRepository.findDistinctDomainsByActiveApis(PageRequest.of(0, limit));
    }

    public List<String> getAvailableOwners(String search, int limit) {
        if (search != null && !search.trim().isEmpty()) {
            return apiRepository.findDistinctOwnersByActiveApisWithSearch(search.trim(), PageRequest.of(0, limit));
        }
        return apiRepository.findDistinctOwnersByActiveApis(PageRequest.of(0, limit));
    }
}
