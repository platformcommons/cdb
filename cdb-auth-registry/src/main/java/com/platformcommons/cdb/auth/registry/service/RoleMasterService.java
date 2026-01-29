package com.platformcommons.cdb.auth.registry.service;

import com.platformcommons.cdb.auth.registry.model.RoleMaster;

import java.util.List;
import java.util.Optional;

public interface RoleMasterService {
    RoleMaster create(RoleMaster role);
    List<RoleMaster> list();
    Optional<RoleMaster> get(Long id);
    Optional<RoleMaster> findByCode(String code);
    RoleMaster update(Long id, RoleMaster input);
    boolean delete(Long id);
    RoleMaster assignAuthoritiesByCodes(Long id, List<String> authorityCodes);
    RoleMaster removeAuthoritiesByCodes(Long id, List<String> authorityCodes);
}
