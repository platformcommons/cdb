package com.platformcommons.cdb.auth.registry.repository;

import com.platformcommons.cdb.auth.registry.model.OAuth2AuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OAuth2AuthorizationCodeRepository extends JpaRepository<OAuth2AuthorizationCode, Long> {
    Optional<OAuth2AuthorizationCode> findByCodeAndUsedFalse(String code);
    void deleteByExpiresAtBefore(Instant expiry);
}