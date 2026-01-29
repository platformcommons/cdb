package com.platformcommons.cdb.platform.api.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Core routing service responsible for determining the target service/endpoint
 * for a given incoming gateway request.
 */
@Service
public class RoutingService {

    //todo.. update and fetch from feign client
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cdb.provider-registry.base-url:http://localhost:8081}")
    private String providerBaseUrl;

    /**
     * Resolves a route configuration for the supplied request. Placeholder implementation.
     *
     * @return a route configuration or null if none matched
     */
    @Cacheable(value = "providerBaseUrls", key = "#providerCode + '-' + #envType", unless = "#bypassCache")
    public String resolveRoute(String providerCode, String envType, boolean bypassCache) {
        return null;
    }
}
