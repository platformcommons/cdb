package com.platformcommons.cdb.platform.api.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DomainStatsResponse {
    private List<DomainStat> domains;
    
    @Getter
    @Setter
    @Builder
    public static class DomainStat {
        private String domain;
        private long apiCount;
    }
}