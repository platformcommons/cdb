package com.platformcommons.cdb.auth.registry.repository;

import com.platformcommons.cdb.auth.registry.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User Repository Interface
 * <p>
 * Spring Data JPA repository for User entity.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}