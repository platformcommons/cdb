package com.platformcommons.cdb.auth.registry.repository;

import com.platformcommons.cdb.auth.registry.model.OAuth2Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, Long> {
    Optional<OAuth2Client> findByClientId(String clientId);
}