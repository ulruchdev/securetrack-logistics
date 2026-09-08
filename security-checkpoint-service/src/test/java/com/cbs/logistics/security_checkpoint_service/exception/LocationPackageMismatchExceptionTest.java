package com.cbs.logistics.security_checkpoint_service.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationPackageMismatchExceptionTest {

    @Test
    void messageShouldContainIds() {
        LocationPackageMismatchException ex = new LocationPackageMismatchException("loc-1", 10L, 20L);
        assertThat(ex.getMessage()).contains("loc-1").contains("10").contains("20");
    }
}
