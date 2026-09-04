package com.cbs.logistics.security_checkpoint_service.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du comportement de l'entité (logique @PrePersist).
 */
class CheckpointLogTest {

    @Test
    void onCreate_shouldSetCheckpointTime_whenNull() {
        CheckpointLog log = CheckpointLog.builder()
                .trackingNumber("ST-ABCDEF12")
                .checkpointId(10L)
                .result(CheckpointResult.OK)
                .build();

        assertThat(log.getCheckpointTime()).isNull();

        log.onCreate();

        assertThat(log.getCheckpointTime()).isNotNull();
        assertThat(log.getCheckpointTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void onCreate_shouldKeepCheckpointTime_whenAlreadySet() {
        LocalDateTime fixed = LocalDateTime.of(2026, 8, 10, 10, 0);
        CheckpointLog log = CheckpointLog.builder()
                .trackingNumber("ST-ABCDEF12")
                .checkpointId(10L)
                .checkpointTime(fixed)
                .result(CheckpointResult.OK)
                .build();

        log.onCreate();

        assertThat(log.getCheckpointTime()).isEqualTo(fixed);
    }
}
