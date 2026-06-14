package com.sportexpo.exhibitor.selection;

import com.sportexpo.exhibitor.common.DemoActors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class BoothSelectionTimeoutJob {
    private final BoothSelectionService service;

    public BoothSelectionTimeoutJob(BoothSelectionService service) {
        this.service = service;
    }

    // fixedRate runs every 10 minutes based on scheduled start time; the job is short and idempotent.
    // initialDelay gives the application 60 seconds to finish schema initialization and warm up.
    @Scheduled(initialDelay = 60_000, fixedRate = 600_000)
    public void releaseExpiredSelections() {
        service.releaseExpiredSelections(OffsetDateTime.now(), DemoActors.SYSTEM_ACTOR_ID);
    }
}
