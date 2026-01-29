package com.platformcommons.cdb.platform.master.data.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple event publisher placeholder. Replace with Spring events, Kafka, etc. later.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    public void publish(MasterDataChangeEvent event) {
        // Placeholder: simply log the event
        log.info("[MDM EVENT] id={}, action={}, at={}", event.getId(), event.getAction(), event.getOccurredAt());
    }
}
