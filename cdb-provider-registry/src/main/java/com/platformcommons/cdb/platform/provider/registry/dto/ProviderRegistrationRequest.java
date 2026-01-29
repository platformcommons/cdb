package com.platformcommons.cdb.platform.provider.registry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO representing a request to register a new provider along with an admin user.
 * <p>
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.2.0
 * Since: 2025-09-15
 */
@Getter
@Setter
@ToString
@Builder
public class ProviderRegistrationRequest {

    // Provider fields
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 120)
    private String code;

    @Size(max = 2048)
    private String description;

    @Email
    private String contactEmail;

    private String contactPhone;

    private String tags;

    @Size(max = 120)
    private String adminUsername;

    @NotBlank
    @Size(min = 8, max = 128)
    private String adminPassword;

    @Size(max = 120)
    private String adminFirstName;

    @Size(max = 120)
    private String adminLastName;

    // OTP verification fields (from step 2)
    @NotBlank
    private String otpKey;

    @NotBlank
    private String otp;
}
