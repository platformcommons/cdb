package com.platformcommons.cdb.auth.registry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderContextOption {
    private Long providerId;
    private String providerCode;
}
