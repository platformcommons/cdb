package com.platformcommons.cdb.platform.api.gateway.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal monitoring/metrics collection service.
 * <p>
 * For production use, integrate with Micrometer and a metrics backend
 * (Prometheus, Cloud Monitoring, etc.).
 * </p>
 */
@Service
public class MonitoringService {

    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    public void increment(String metric) {
        counters.merge(metric, 1L, Long::sum);
    }

    public long counter(String metric) {
        return counters.getOrDefault(metric, 0L);
    }

    public long time(String metric, Runnable runnable) {
        Instant start = Instant.now();
        try {
            runnable.run();
        } finally {
            long ms = Duration.between(start, Instant.now()).toMillis();
            counters.merge(metric + ".count", 1L, Long::sum);
            counters.merge(metric + ".totalMs", ms, Long::sum);
        }
        return Duration.between(start, Instant.now()).toMillis();
    }
}
