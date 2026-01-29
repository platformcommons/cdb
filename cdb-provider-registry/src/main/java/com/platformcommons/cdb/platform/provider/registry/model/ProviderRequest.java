package com.platformcommons.cdb.platform.provider.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "cdb_user_provider_requests")
@Getter
@Setter
@ToString
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProviderRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private String providerCode;

    @Column(length = 1000)
    private String requestMessage;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column
    private Long approvedBy;

    @Column(length = 1000)
    private String approvalNotes;

    @Column
    private String requestedRole = "USER";
}