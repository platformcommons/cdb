package com.platformcommons.cdb.platform.api.gateway.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Defines request/response transformation rules applied by the gateway.
 */
@Getter
@Setter
@ToString
@Builder
public class TransformationRule {
    private Map<String, String> addHeaders;
    private List<String> removeHeaders;
    private String rewriteRegex;
    private String rewriteReplacement;
}
