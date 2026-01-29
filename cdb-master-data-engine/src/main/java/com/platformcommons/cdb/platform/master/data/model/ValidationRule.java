package com.platformcommons.cdb.platform.master.data.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a validation rule to be applied to master data entries.
 */
@Getter
@Setter
@ToString
@Builder
public class ValidationRule {
    private String id;
    private String name;
    private String expression;
    private String message;
}
