package com.platformcommons.cdb.platform.api.registry.dto;

import com.platformcommons.cdb.platform.api.registry.model.Api;
import com.platformcommons.cdb.platform.api.registry.model.ApiAuditLog;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ApiDetailResponse {
    private Api api;
    private List<ApiAuditLog> auditHistory;
    private List<String> availableVersions;
}