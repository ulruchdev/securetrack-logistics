package com.cbs.logistics.tracking_service.query;

import com.cbs.logistics.tracking_service.event.TrackingTransitionedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.config.ProcessingGroup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ProcessingGroup("tracking-history")
public class TrackingHistoryProjection {

    private final TrackingHistoryRepository repository;

    public TrackingHistoryProjection(TrackingHistoryRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(TrackingTransitionedEvent event) {
        try {
            repository.save(new TrackingHistoryEntry(
                    event.packageId(),
                    event.locationId(),
                    event.newStatus(),
                    event.occurredAt()
            ));
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate event ignored: package {} status {} at {}",
                    event.packageId(), event.newStatus(), event.occurredAt());
        }
    }
}
