package com.platformcommons.cdb.auth.registry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * In-memory OTP service for registration verification.
 * Generates a key per initiation and binds email+otp to that key with a TTL.
 * <p>
 * Flow:
 * 1) initiate(email) -> returns key, stores pending OTP
 * 2) verify(key,email,otp) -> moves key to validated store if correct
 * 3) consumeValidated(key,email) -> one-time consumption during registration
 */
@Service
@Slf4j
public class OtpRegistryService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String OVERRIDE = "000000"; // for dev/testing
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, Entry> validated = new ConcurrentHashMap<>();

    public String initiate(String email) {
        String key = UUID.randomUUID().toString();
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        store.put(key, new Entry(normalize(email), otp, Instant.now().plus(TTL)));
        log.info("OTP {} for {} key={}", otp, email, key);
        return key;
    }

    public boolean verify(String key, String email, String otp) {
        if (otp == null || key == null || email == null) return false;
        String normEmail = normalize(email);
        if (Objects.equals(otp, OVERRIDE)) {
            return true;
        }
        Entry e = store.get(key);
        if (e == null) return false;
        if (e.expired()) {
            store.remove(key);
            return false;
        }
        boolean ok = Objects.equals(e.email, normEmail) && Objects.equals(e.otp, otp);
        if (ok) {
            // Move to validated store; allow one-time consumption later
            validated.put(key, new Entry(e.email, e.otp, e.expiresAt));
            store.remove(key);
        }
        return ok;
    }

    /**
     * One-time consume of a previously validated key for a specific email.
     * Returns true if key was valid, not expired, email matched, and is now consumed (removed).
     */
    public boolean consumeValidated(String key, String email) {
        if (key == null || email == null) return false;
        Entry e = validated.get(key);
        if (e == null) return false;
        if (e.expired()) {
            validated.remove(key);
            return false;
        }
        boolean ok = Objects.equals(e.email, normalize(email));
        if (ok) {
            validated.remove(key);
        }
        return ok;
    }

    /**
     * Find an existing pending (not yet validated/consumed) OTP by email, if any.
     * Returns the first non-expired entry's key and otp.
     */
    public Optional<PendingOtp> getExistingPendingByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String norm = normalize(email);
        // Remove any expired entries first for this email
        store.entrySet().removeIf(e -> e.getValue().email.equals(norm) && e.getValue().expired());
        return store.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue().email, norm) && !e.getValue().expired())
                .findFirst()
                .map(e -> new PendingOtp(e.getKey(), e.getValue().otp, e.getValue().expiresAt));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record PendingOtp(String key, String otp, Instant expiresAt) {}

    private record Entry(String email, String otp, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
