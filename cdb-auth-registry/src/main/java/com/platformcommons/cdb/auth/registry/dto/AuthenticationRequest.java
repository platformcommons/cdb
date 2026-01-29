package com.platformcommons.cdb.auth.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Authentication Request DTO
 * 
 * Data transfer object for authentication requests.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 */
@Getter
@Setter
@ToString
@Builder
public class AuthenticationRequest {
    private String email;
    private String password;
    private String mfaCode;
}