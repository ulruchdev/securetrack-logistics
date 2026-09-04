package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.package_service.entity.EventOutbox;
import com.cbs.logistics.package_service.repository.EventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventOutboxServiceTest {

    private static final String TENANT_ID = "test-tenant";

    @Mock
    private EventOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventOutboxService eventOutboxService;

    @BeforeEach
    void setUp() throws Exception {
        TenantContext.setCurrent(TENANT_ID);
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":true}");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void storeEvent_shouldSavePendingEvent() throws Exception {
        eventOutboxService.storeEvent("TestEvent", "test.routing", "payload");

        ArgumentCaptor<EventOutbox> captor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(outboxRepository).save(captor.capture());

        EventOutbox saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TestEvent");
        assertThat(saved.getRoutingKey()).isEqualTo("test.routing");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void storePackageStatusChanged_shouldStoreEvent() throws Exception {
        PackageStatusChangedEvent event = new PackageStatusChangedEvent(
                1L, "NEW", "IN_TRANSIT", null, Instant.now()
        );

        eventOutboxService.storePackageStatusChanged(event);

        ArgumentCaptor<EventOutbox> captor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(outboxRepository).save(captor.capture());

        EventOutbox saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("PackageStatusChanged");
        assertThat(saved.getRoutingKey()).isEqualTo("status.changed");
    }

    @Test
    void publishPendingEvents_shouldSendToRabbitAndMarkPublished() {
        EventOutbox event = EventOutbox.builder()
                .id(1L)
                .eventType("PackageStatusChanged")
                .routingKey("status.changed")
                .payload("{\"packageId\":1}")
                .status("PENDING")
                .createdAt(Instant.now())
                .retryCount(0)
                .tenantId(TENANT_ID)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        eventOutboxService.publishPendingEvents();

        verify(rabbitTemplate).convertAndSend("package-status", "status.changed", "{\"packageId\":1}");
        verify(outboxRepository).markAsPublished(1L);
    }

    @Test
    void publishPendingEvents_shouldMarkAsFailed_WhenRabbitThrows() {
        EventOutbox event = EventOutbox.builder()
                .id(2L)
                .eventType("TestEvent")
                .routingKey("test")
                .payload("{}")
                .status("PENDING")
                .createdAt(Instant.now())
                .retryCount(0)
                .tenantId(TENANT_ID)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(rabbitTemplate)
                .convertAndSend(eq("package-status"), eq("status.changed"), any(String.class));

        eventOutboxService.publishPendingEvents();

        verify(outboxRepository).markAsFailed(2L);
        verify(outboxRepository, never()).markAsPublished(2L);
    }

    @Test
    void publishPendingEvents_shouldDoNothing_WhenNoPendingEvents() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of());

        eventOutboxService.publishPendingEvents();

        verifyNoInteractions(rabbitTemplate);
        verify(outboxRepository, never()).markAsPublished(any());
        verify(outboxRepository, never()).markAsFailed(any());
    }
}
