package com.platformcommons.cdb.auth.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * User Registration Request DTO
 * <p>
 * Data transfer object for user registration requests.
 * <p>
 * Now also includes a messageKey that must be a previously validated OTP key.
 * Registration will only proceed if the key has been validated and is consumed.
 *
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.1.0
 */
@Getter
@Setter
@ToString
@Builder
public class UserRegistrationRequest {
    private String otpKey;
    private String otp;
    private String username;
    private String email;
    private String password;
    private Long providerId;
    private String providerCode;
}