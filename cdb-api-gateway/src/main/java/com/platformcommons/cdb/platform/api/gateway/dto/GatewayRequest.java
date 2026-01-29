package com.platformcommons.cdb.platform.api.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Representation of an incoming gateway request after normalization.
 */
@Getter
@Setter
@ToString
@Builder
public class GatewayRequest {
    private String method;
    private String path;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;
}
