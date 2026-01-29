package com.platformcommons.cdb.platform.api.gateway.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple client-side load balancer using round-robin strategy.
 * Replace with service discovery or external LB integration as needed.
 */
@Service
public class LoadBalancerService {

    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Selects an instance from the provided list using round-robin.
     * @param instances list of target instance base URLs
     * @return selected instance URL or null if input list is empty
     */
    public String chooseInstance(List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int idx = Math.abs(counter.getAndIncrement() % instances.size());
        return instances.get(idx);
    }
}
