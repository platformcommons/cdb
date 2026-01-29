package com.platformcommons.cdb.auth.registry.repository;

import com.platformcommons.cdb.auth.registry.model.AuthorityMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityMasterRepository extends JpaRepository<AuthorityMaster, Long> {
    Optional<AuthorityMaster> findByCode(String code);
}
