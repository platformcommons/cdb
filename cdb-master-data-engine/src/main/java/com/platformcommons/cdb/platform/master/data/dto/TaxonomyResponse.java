package com.platformcommons.cdb.platform.master.data.dto;

import com.platformcommons.cdb.platform.master.data.model.Taxonomy;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * DTO representing a taxonomy response with domain and nodes.
 */
@Getter
@Setter
@ToString
@Builder
public class TaxonomyResponse {
    private String domain;
    private List<Taxonomy> nodes;

    public static TaxonomyResponse of(String domain, List<Taxonomy> nodes) {
        return TaxonomyResponse.builder()
                .domain(domain)
                .nodes(nodes)
                .build();
    }
}
