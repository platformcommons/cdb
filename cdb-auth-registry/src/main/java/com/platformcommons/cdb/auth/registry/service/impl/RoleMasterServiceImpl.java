package com.platformcommons.cdb.auth.registry.service.impl;

import com.platformcommons.cdb.auth.registry.model.AuthorityMaster;
import com.platformcommons.cdb.auth.registry.model.RoleMaster;
import com.platformcommons.cdb.auth.registry.repository.AuthorityMasterRepository;
import com.platformcommons.cdb.auth.registry.repository.RoleMasterRepository;
import com.platformcommons.cdb.auth.registry.service.RoleMasterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class RoleMasterServiceImpl implements RoleMasterService {

    private final RoleMasterRepository roleRepo;
    private final AuthorityMasterRepository authRepo;

    public RoleMasterServiceImpl(RoleMasterRepository roleRepo, AuthorityMasterRepository authRepo) {
        this.roleRepo = roleRepo;
        this.authRepo = authRepo;
    }

    @Override
    public RoleMaster create(RoleMaster role) {
        return roleRepo.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleMaster> list() {
        return roleRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleMaster> get(Long id) {
        return roleRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleMaster> findByCode(String code) {
        return roleRepo.findByCode(code);
    }

    @Override
    public RoleMaster update(Long id, RoleMaster input) {
        RoleMaster existing = roleRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Role not found"));
        existing.setCode(input.getCode());
        existing.setLabel(input.getLabel());
        existing.setType(input.getType());
        if (input.getAuthorities() != null) {
            existing.getAuthorities().clear();
            existing.getAuthorities().addAll(input.getAuthorities());
        }
        return roleRepo.save(existing);
    }

    @Override
    public boolean delete(Long id) {
        if (!roleRepo.existsById(id)) return false;
        roleRepo.deleteById(id);
        return true;
    }

    @Override
    public RoleMaster assignAuthoritiesByCodes(Long id, List<String> authorityCodes) {
        RoleMaster role = roleRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Role not found"));
        if (authorityCodes != null && !authorityCodes.isEmpty()) {
            List<AuthorityMaster> found = authorityCodes.stream()
                    .map(authRepo::findByCode)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            role.getAuthorities().addAll(found);
            role = roleRepo.save(role);
        }
        return role;
    }

    @Override
    public RoleMaster removeAuthoritiesByCodes(Long id, List<String> authorityCodes) {
        RoleMaster role = roleRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Role not found"));
        if (authorityCodes != null && !authorityCodes.isEmpty()) {
            Set<String> set = new HashSet<>(authorityCodes);
            role.getAuthorities().removeIf(a -> a.getCode() != null && set.contains(a.getCode()));
            role = roleRepo.save(role);
        }
        return role;
    }
}
