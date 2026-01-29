package com.platformcommons.cdb.auth.registry.service;

import com.platformcommons.cdb.auth.registry.model.User;
import com.platformcommons.cdb.auth.registry.dto.UserRegistrationRequest;
import com.platformcommons.cdb.auth.registry.model.UserProviderMapping;

import java.util.List;
import java.util.Optional;

/**
 * User Management Service
 * 
 * Service for managing user accounts, registration, and profile operations.
 * 
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 */
public interface UserManagementService {
    
    /**
     * Register a new user
     * @param request the user registration request
     * @return the created user
     */
    User registerUser(UserRegistrationRequest request);
    
    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String username);

    
    /**
     * Update user profile
     * @param userId the user ID
     * @param user the updated user data
     * @return the updated user
     */
    User updateUser(Long userId, User user);
    
    /**
     * Enable or disable user account
     * @param userId the user ID
     * @param enabled the enabled status
     */
    void setUserEnabled(Long userId, boolean enabled);
    

    
    /**
     * Delete user account
     * @param userId the user ID to delete
     */
    void deleteUser(Long userId);

}