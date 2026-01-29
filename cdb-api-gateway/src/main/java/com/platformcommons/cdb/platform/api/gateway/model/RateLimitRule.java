package com.platformcommons.cdb.platform.api.gateway.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Encapsulates a rate limit rule for a route or identity.
 */
@Getter
@Setter
@ToString
@Builder
public class RateLimitRule {
    private String key; // e.g., userId, apiKey, ip
    private long limit; // max requests
    private long windowSeconds; // per window
}
