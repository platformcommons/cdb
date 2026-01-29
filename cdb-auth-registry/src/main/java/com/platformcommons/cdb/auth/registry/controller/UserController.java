package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.dto.UserRegistrationRequest;
import com.platformcommons.cdb.auth.registry.model.User;
import com.platformcommons.cdb.auth.registry.service.UserManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 * <p>
 * REST controller for user management operations including registration,
 * profile updates, and user administration.
 *
 * @author Aashish Aadarsh (aashish@platformcommons.com)
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Register new user
     *
     * @param request user registration request
     * @return created user
     */
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationRequest request) {
        User user = userManagementService.registerUser(request);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username
     *
     * @param email the username
     * @return user if found
     */
    @GetMapping("/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userManagementService.findByEmail(email)
                .orElseThrow());
    }

    /**
     * Check if a user exists by email
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByUsername(@RequestParam String username) {
        boolean exists = userManagementService.findByEmail(username).isPresent();
        return ResponseEntity.ok(exists);
    }

    /**
     * Update user profile
     *
     * @param userId the user ID
     * @param user   updated user data
     * @return updated user
     */
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody User user) {
        User updatedUser = userManagementService.updateUser(userId, user);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Enable or disable user account
     *
     * @param userId  the user ID
     * @param enabled the enabled status
     * @return success response
     */
    @PatchMapping("/{userId}/enabled")
    public ResponseEntity<Void> setUserEnabled(@PathVariable Long userId, @RequestParam boolean enabled) {
        userManagementService.setUserEnabled(userId, enabled);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete user account
     *
     * @param userId the user ID
     * @return success response
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}