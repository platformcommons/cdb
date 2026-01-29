package com.platformcommons.cdb.platform.api.gateway.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Represents a route configuration in the API Gateway.
 */
@Getter
@Setter
@ToString
@Builder
public class RouteConfiguration {
    private String id;
    private String pathPattern; // e.g., /api/v1/items/**
    private List<String> methods; // GET, POST, etc.
    private List<String> targetInstances; // base URLs of target service instances
    private int timeoutMs;
    private int retryCount;
}
