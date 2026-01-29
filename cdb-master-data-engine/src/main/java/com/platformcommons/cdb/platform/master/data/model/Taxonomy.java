package com.platformcommons.cdb.platform.master.data.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Represents a taxonomy node in a hierarchical structure.
 */
@Getter
@Setter
@ToString
@Builder
public class Taxonomy {
    private String id;
    private String domain;
    private String code;
    private String name;
    private String parentId;
    private List<Taxonomy> children;
}
