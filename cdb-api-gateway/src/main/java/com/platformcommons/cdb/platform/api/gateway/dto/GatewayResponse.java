package com.platformcommons.cdb.platform.api.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Standardized response returned by the gateway after invoking a downstream service.
 */
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class GatewayResponse {
    private int status;
    private Map<String, String> headers;
    private String body;
}
