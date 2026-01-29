package com.platformcommons.cdb.auth.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Authority master entity.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "authority_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_authority_master_code", columnNames = {"code"})
        })
public class AuthorityMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String code; // unique

    @Column(name = "process_area", length = 150)
    private String processArea;
}
