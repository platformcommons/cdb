package com.platformcommons.cdb.auth.registry.model;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "oauth2_clients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth2_clients_client_id", columnNames = {"client_id"})
        })
public class OAuth2Client extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_redirect_uris", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri", length = 500)
    private Set<String> redirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_scopes", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope", length = 100)
    private Set<String> scopes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_grant_types", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type", length = 50)
    private Set<String> grantTypes;

    @Column(name = "require_pkce", nullable = false)
    private boolean requirePkce = true;

    @Column(name = "require_consent", nullable = false)
    private boolean requireConsent = true;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "description", length = 1000)
    private String description;
}