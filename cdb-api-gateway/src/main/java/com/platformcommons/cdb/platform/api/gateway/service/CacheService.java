package com.platformcommons.cdb.platform.api.gateway.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache service placeholder for gateway responses.
 * <p>
 * This is not suitable for production multi-node deployments. Replace with a
 * distributed cache (e.g., Redis) and proper cache invalidation strategies.
 * </p>
 */
@Service
public class CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public void put(String key, String payload, long ttlSeconds) {
        cache.put(key, new CacheEntry(payload, Instant.now().plusSeconds(ttlSeconds)));
    }

    public Optional<String> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.payload());
    }

    public void evict(String key) {
        cache.remove(key);
    }

    private record CacheEntry(String payload, Instant expiresAt) {
        @Override public boolean equals(Object o) { return this == o; }
        @Override public int hashCode() { return Objects.hash(expiresAt); }
    }
}
