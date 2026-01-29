package com.platformcommons.cdb.platform.api.registry.dto;

import com.platformcommons.cdb.platform.api.registry.model.Api;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ApiListResponse {
    private List<ApiSummary> apis;
    private int totalElements; // for Page; may be -1 when using Slice
    private int totalPages; // for Page; may be -1 when using Slice
    private int currentPage;
    private int pageSize;
    private boolean hasNext; // for Slice pagination
    private int numberOfElements; // elements in current slice
    
    @Getter
    @Setter
    @Builder
    public static class ApiSummary {
        private Long id;
        private String name;
        private String description;
        private String version;
        private String status;
        private String owner;
        private java.time.Instant updatedAt;
        private List<String> tags;
        private List<String> domains;
    }
}