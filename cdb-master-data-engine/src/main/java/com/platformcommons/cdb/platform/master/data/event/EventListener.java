package com.platformcommons.cdb.platform.master.data.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Example event listener placeholder. Replace with actual subscription logic when integrating a bus.
 */
@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    public void onMasterDataChange(MasterDataChangeEvent event) {
        // Placeholder: simply log
        log.info("[MDM EVENT LISTENER] Received event id={}, action={}", event.getId(), event.getAction());
    }
}
