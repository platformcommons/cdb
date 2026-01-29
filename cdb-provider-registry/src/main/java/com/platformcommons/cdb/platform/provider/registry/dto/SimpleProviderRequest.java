package com.platformcommons.cdb.platform.provider.registry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
@Builder
public class SimpleProviderRequest {
    private String name;
    private String code;
    private String description;
    private String contactEmail;
    private String contactPhone;
}
