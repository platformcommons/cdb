package com.platformcommons.cdb.platform.provider.registry.model;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * ProviderMetadata represents additional metadata associated with a Provider entity,
 * such as contact information and classification tags.
 *
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@Embeddable
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMetadata {

    /** Primary email for the provider organization */
    private String contactEmail;

    /** Contact phone number for the provider organization */
    private String contactPhone;

    /** Comma separated list of tags or categories for lightweight filtering */
    private String tags;
}
