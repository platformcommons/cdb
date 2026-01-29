package com.platformcommons.cdb.auth.registry.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutiveContextRequest {
    private String providerCode;
}
