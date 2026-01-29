package com.platformcommons.cdb.auth.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Role master entity.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "role_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_master_code", columnNames = {"code"})
        })
public class RoleMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code; // unique

    @Column(nullable = false, length = 150)
    private String label;

    @Column(length = 100)
    private String type;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_authorities",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id"))
    @Builder.Default
    private Set<AuthorityMaster> authorities = new HashSet<>();
}
