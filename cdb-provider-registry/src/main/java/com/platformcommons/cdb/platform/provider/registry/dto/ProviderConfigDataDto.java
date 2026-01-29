package com.platformcommons.cdb.platform.provider.registry.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfigDataDto {
    private Long id;
    private String configValue;
    private Integer configValueSequence;
}