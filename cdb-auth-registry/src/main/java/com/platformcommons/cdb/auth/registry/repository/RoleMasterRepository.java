package com.platformcommons.cdb.auth.registry.repository;

import com.platformcommons.cdb.auth.registry.model.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleMasterRepository extends JpaRepository<RoleMaster, Long> {
    Optional<RoleMaster> findByCode(String code);
}
