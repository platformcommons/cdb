package com.platformcommons.cdb.auth.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Token Response DTO
 * 
 * Data transfer object for JWT token responses.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 */
@Getter
@Setter
@ToString
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}