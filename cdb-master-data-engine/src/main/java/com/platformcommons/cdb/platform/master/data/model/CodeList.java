package com.platformcommons.cdb.platform.master.data.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a code in a code list (reference values).
 */
@Getter
@Setter
@ToString
@Builder
public class CodeList {
    private String id;
    private String category;
    private String code;
    private String description;
}
