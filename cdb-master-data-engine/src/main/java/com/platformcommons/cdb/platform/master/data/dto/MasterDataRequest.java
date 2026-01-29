package com.platformcommons.cdb.platform.master.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Request DTO for creating/updating MasterData records.
 */
@Getter
@Setter
@ToString
@Builder
public class MasterDataRequest {
    private String id; // optional for update
    private String type;
    private Map<String, Object> attributes;
}
