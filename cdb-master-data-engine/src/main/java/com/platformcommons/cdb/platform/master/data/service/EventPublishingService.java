package com.platformcommons.cdb.platform.master.data.service;

import com.platformcommons.cdb.platform.master.data.event.EventPublisher;
import com.platformcommons.cdb.platform.master.data.event.MasterDataChangeEvent;
import org.springframework.stereotype.Service;

/**
 * Facade service that publishes domain events for master data changes.
 */
@Service
public class EventPublishingService {

    private final EventPublisher eventPublisher;

    public EventPublishingService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes a change event for a given MasterData id. Placeholder only enqueues a simple event.
     */
    public void publishChange(String masterDataId, String action) {
        MasterDataChangeEvent event = new MasterDataChangeEvent();
        event.setId(masterDataId);
        event.setAction(action);
        eventPublisher.publish(event);
    }
}
