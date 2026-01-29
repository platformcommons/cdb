package com.platformcommons.cdb.common.openapi;

import org.springdoc.core.models.GroupedOpenApi;

/**
 * Base OpenAPI configuration utility to share common grouping helpers across modules.
 */
public class BaseOpenApiConfig {

    /**
     * Create a GroupedOpenApi bean with the given group name and path patterns.
     * Modules can call this in their @Configuration classes to expose grouped specs.
     */
    protected GroupedOpenApi groupedApi(String group, String... pathsToMatch) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch(pathsToMatch)
                .build();
    }
}
