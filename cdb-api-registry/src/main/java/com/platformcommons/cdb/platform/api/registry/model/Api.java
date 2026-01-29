package com.platformcommons.cdb.platform.api.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Domain model representing an API registered in the CDB platform.
 */
@Entity
@Table(name = "cdb_api", indexes = {
    @Index(name = "idx_api_name", columnList = "name"),
    @Index(name = "idx_api_owner", columnList = "owner"),
    @Index(name = "idx_api_status", columnList = "status")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Api extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(length = 36)
    private Long id;

    private String name;
    private String owner;
    private String description;
    @Lob
    @Column(name = "detailed_description", columnDefinition = "LONGTEXT")
    private String detailedDescription;
    @Column(name = "base_path")
    private String basePath;
    private String version;

    @Column(name = "status")
    private String status;
    @Lob
    @Column(name = "open_api_spec", columnDefinition = "LONGTEXT")
    private String openApiSpec;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cdb_api_tags",
            joinColumns = @JoinColumn(name = "api_id"),
            indexes = @Index(name = "idx_api_tags_value", columnList = "tags")
    )
    private List<String> tags;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cdb_api_domains",
            joinColumns = @JoinColumn(name = "api_id"),
            indexes = @Index(name = "idx_api_domains_value", columnList = "domains")
    )
    private List<String> domains;

    public enum ApiStatus {
        DRAFT, PUBLISHED, DEPRECATED, RETIRED
    }
}
