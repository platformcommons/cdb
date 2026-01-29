package com.platformcommons.cdb.platform.provider.registry.mapper;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderResponse;
import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import org.springframework.stereotype.Component;

/**
 * Mapper for Provider entity and DTOs.
 * Centralizes mapping logic between entity and API layer objects.
 */
@Component
public class ProviderMapper {

    public ProviderResponse toResponse(Provider p) {
        if (p == null) return null;
        return getProviderResponse(p);
    }

    private ProviderResponse getProviderResponse(Provider p) {
        ProviderResponse r = new ProviderResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setCode(p.getCode());
        r.setDescription(p.getDescription());
        r.setStatus(p.getStatus());
        if (p.getMetadata() != null) {
            r.setContactEmail(p.getMetadata().getContactEmail());
            r.setContactPhone(p.getMetadata().getContactPhone());
            r.setTags(p.getMetadata().getTags());
        }
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
