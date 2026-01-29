package com.platformcommons.cdb.platform.api.registry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platformcommons.cdb.platform.api.registry.dto.ApiDetailResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiListResponse;
import com.platformcommons.cdb.platform.api.registry.dto.ApiRegistrationRequest;
import com.platformcommons.cdb.platform.api.registry.dto.ApiSearchRequest;
import com.platformcommons.cdb.platform.api.registry.dto.DomainStatsResponse;
import com.platformcommons.cdb.platform.api.registry.mapper.ApiMapper;
import com.platformcommons.cdb.platform.api.registry.model.Api;
import com.platformcommons.cdb.platform.api.registry.model.ApiAuditLog;
import com.platformcommons.cdb.platform.api.registry.repository.ApiAuditLogRepository;
import com.platformcommons.cdb.platform.api.registry.repository.ApiRepository;
import com.platformcommons.cdb.security.auth.CDBSecurityUtil;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiManagementService {

    private final ApiRepository apiRepository;
    private final ApiAuditLogRepository auditRepository;
    private final CDBSecurityUtil cdbSecurityUtil;
    private final ObjectMapper objectMapper;

    public ApiManagementService(ApiRepository apiRepository,
                                ApiAuditLogRepository auditRepository, CDBSecurityUtil cdbSecurityUtil, ObjectMapper objectMapper) {
        this.apiRepository = apiRepository;
        this.auditRepository = auditRepository;
        this.cdbSecurityUtil = cdbSecurityUtil;
        this.objectMapper = objectMapper;
    }

    public Long registerApi(ApiRegistrationRequest request) {
        Api api = ApiMapper.toNewEntity(request);
        /*addAudit(id, api.getVersion(), "CREATE", Map.of(), createValueMap(api),
                "Initial registration");*/
        return apiRepository.save(api).getId();
    }

    public void updateApi(Long apiId, ApiRegistrationRequest request) {
        Api existingApi = requireApi(apiId);
        
        boolean isPublished = "PUBLISHED".equals(existingApi.getStatus());
        String nextVersion = isPublished ? incrementVersion(existingApi.getVersion()) : existingApi.getVersion();
        
        Api updatedApi = ApiMapper.applyUpdate(existingApi, request, nextVersion);
        apiRepository.save(updatedApi);
        
        if (isPublished) {
            Map<String, Object> oldValues = createValueMap(existingApi);
            Map<String, Object> newValues = createValueMap(updatedApi);
            addAudit(apiId, nextVersion, "UPDATE", oldValues, newValues,
                    diffDescription(oldValues, newValues));
        }
    }

    public ApiListResponse getApis(Pageable pageable, String status) {
        List<Api> filteredApis = apiRepository.findAllByCreatedByProvider(pageable,cdbSecurityUtil.getCurrentProviderId()).stream().toList();
        List<ApiListResponse.ApiSummary> summaries = filteredApis.stream()
                .map(api -> ApiListResponse.ApiSummary.builder()
                        .id(api.getId())
                        .name(api.getName())
                        .description(api.getDescription())
                        .version(api.getVersion())
                        .status(api.getStatus())
                        .owner(api.getOwner())
                        .updatedAt(api.getUpdatedAt())
                        .tags(api.getTags())
                        .domains(api.getDomains())
                        .build())
                .collect(Collectors.toList());

        return ApiListResponse.builder()
                .apis(summaries)
                .totalElements(filteredApis.size())
                .totalPages((int) Math.ceil((double) filteredApis.size() / Math.max(1, pageable.getPageSize())))
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .build();
    }

    public ApiDetailResponse getApiDetail(Long apiId) {
        Api api = requireApi(apiId);

      //  List<ApiAuditLog> auditHistory = new ArrayList<>(auditRepository.findByApiId(apiId));
       /* List<String> versions = auditHistory.stream()
                .map(ApiAuditLog::getVersion)
                .distinct()
                .collect(Collectors.toList());*/

        return ApiDetailResponse.builder()
                .api(api)
               // .auditHistory(auditHistory)
               // .availableVersions(versions)
                .build();
    }

    private Api requireApi(Long apiId) {
        return apiRepository.findById(apiId).orElseThrow(()
                -> new RuntimeException("API not found"));
    }

    private String incrementVersion(String version) {
        if (version == null || version.isBlank()) return "1.0.0";
        String[] parts = version.split("\\.");
        try {
            int patchIndex = parts.length - 1;
            int patch = Integer.parseInt(parts[patchIndex]);
            parts[patchIndex] = String.valueOf(patch + 1);
            return String.join(".", parts);
        } catch (Exception e) {
            return version + ".1";
        }
    }

    private Map<String, Object> createValueMap(Api api) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", api.getId());
        map.put("name", api.getName());
        map.put("owner", api.getOwner());
        map.put("description", api.getDescription());
        map.put("detailedDescription", api.getDetailedDescription());
        map.put("basePath", api.getBasePath());
        map.put("version", api.getVersion());
        map.put("status", api.getStatus());
        map.put("openApiSpec", api.getOpenApiSpec());
        map.put("tags", api.getTags() == null ? List.of() : new ArrayList<>(api.getTags()));
        map.put("domains", api.getDomains() == null ? List.of() : new ArrayList<>(api.getDomains()));
        map.put("createdAt", api.getCreatedAt());
        map.put("updatedAt", api.getUpdatedAt());
        map.put("createdBy", api.getCreatedBy());
        map.put("updatedBy", api.getUpdatedBy());
        return map;
    }

    @SneakyThrows
    private void addAudit(Long apiId, String version, String action,
                          Map<String, Object> oldValues, Map<String, Object> newValues,
                          String description) {
        ApiAuditLog log = ApiAuditLog.builder()
                .apiId(apiId)
                .version(version)
                .action(action)
                .changedBy(cdbSecurityUtil.getCurrentUserLogin())
                .changedAt(Instant.now())
                .oldValuesJson(objectMapper.writeValueAsString(oldValues))
                .newValuesJson(objectMapper.writeValueAsString(newValues))
                .changeDescription(description)
                .build();
        auditRepository.save(log);
    }

    private String diffDescription(Map<String, Object> oldValues, Map<String, Object> newValues) {
        List<String> diffs = new ArrayList<>();
        for (String key : new TreeSet<>(newValues.keySet())) {
            Object o = oldValues.get(key);
            Object n = newValues.get(key);
            if (!Objects.equals(o, n)) {
                diffs.add(key);
            }
        }
        return diffs.isEmpty() ? "No changes" : ("Changed: " + String.join(", ", diffs));
    }
    
    public ApiListResponse searchApis(String query, List<String> tags, List<String> domains, String owner, String status, Pageable pageable) {
        List<String> filterTags = CollectionUtils.isEmpty(tags) ? null : tags;
        List<String> filterDomains = CollectionUtils.isEmpty(domains) ? null : domains;
        
        // Default to non-draft statuses if no status specified
        List<String> statusList = status==null ? null : Arrays.asList(status.split(","));
        
        Page<Api> apiPage = apiRepository.searchApis(query, filterTags, filterDomains, owner, statusList, pageable);
        
        List<ApiListResponse.ApiSummary> summaries = apiPage.getContent().stream()
                .map(api -> ApiListResponse.ApiSummary.builder()
                        .id(api.getId())
                        .name(api.getName())
                        .description(api.getDescription())
                        .version(api.getVersion())
                        .status(api.getStatus())
                        .owner(api.getOwner())
                        .updatedAt(api.getUpdatedAt())
                        .tags(api.getTags())
                        .domains(api.getDomains())
                        .build())
                .collect(Collectors.toList());

        return ApiListResponse.builder()
                .apis(summaries)
                .totalElements((int) apiPage.getTotalElements())
                .totalPages(apiPage.getTotalPages())
                .currentPage(apiPage.getNumber())
                .pageSize(apiPage.getSize())
                .build();
    }

    public DomainStatsResponse getDomainStats() {
        List<Object[]> results = apiRepository.findDomainStats();
        List<DomainStatsResponse.DomainStat> stats = results.stream()
                .map(row -> DomainStatsResponse.DomainStat.builder()
                        .domain((String) row[0])
                        .apiCount((Long) row[1])
                        .build())
                .collect(Collectors.toList());
        return DomainStatsResponse.builder().domains(stats).build();
    }

    public ApiListResponse getApisByDomain(String domain, Pageable pageable) {
        Page<Api> apiPage = apiRepository.findByDomainsContainingAndStatusNot(domain, "DRAFT", pageable);
        
        List<ApiListResponse.ApiSummary> summaries = apiPage.getContent().stream()
                .map(api -> ApiListResponse.ApiSummary.builder()
                        .id(api.getId())
                        .name(api.getName())
                        .description(api.getDescription())
                        .version(api.getVersion())
                        .status(api.getStatus())
                        .owner(api.getOwner())
                        .updatedAt(api.getUpdatedAt())
                        .tags(api.getTags())
                        .domains(api.getDomains())
                        .build())
                .collect(Collectors.toList());

        return ApiListResponse.builder()
                .apis(summaries)
                .totalElements((int) apiPage.getTotalElements())
                .totalPages(apiPage.getTotalPages())
                .currentPage(apiPage.getNumber())
                .pageSize(apiPage.getSize())
                .build();
    }

    public List<String> searchTags(String query) {
        return apiRepository.findDistinctTagsContaining(query.toLowerCase())
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<String> searchDomains(String query) {
        return apiRepository.findDistinctDomainsContaining(query.toLowerCase())
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }
}
