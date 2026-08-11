package com.cbs.logistics.location_service.client;

import com.cbs.logistics.location_service.exception.PackageNotFoundException;
import com.cbs.logistics.location_service.exception.PackageServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de traduction des erreurs HTTP du Package Service
 * (le service ne doit jamais voir de types Feign - Clean Architecture).
 */
class PackageServiceErrorDecoderTest {

    private final PackageServiceErrorDecoder decoder = new PackageServiceErrorDecoder();

    private Response response(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://localhost:8081/api/packages/1",
                Collections.emptyMap(),
                new byte[0],
                null,
                null
        );
        return Response.builder()
                .status(status)
                .reason("reason")
                .request(request)
                .headers(Collections.emptyMap())
                .body(new byte[0])
                .build();
    }

    @Test
    void decode_shouldReturnPackageNotFound_when404() {
        Exception result = decoder.decode("PackageServiceClient#getPackageById(Long)", response(404));

        assertThat(result)
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessage("Le colis demandé n'existe pas");
    }

    @Test
    void decode_shouldReturnPackageServiceUnavailable_when500() {
        Exception result = decoder.decode("PackageServiceClient#getPackageById(Long)", response(500));

        assertThat(result)
                .isInstanceOf(PackageServiceUnavailableException.class)
                .hasMessage("Le service de colis est indisponible");
    }

    @Test
    void decode_shouldReturnPackageServiceUnavailable_when503() {
        Exception result = decoder.decode("PackageServiceClient#getPackageById(Long)", response(503));

        assertThat(result).isInstanceOf(PackageServiceUnavailableException.class);
    }

    @Test
    void decode_shouldDelegateToDefaultDecoder_forOtherStatus() {
        Exception result = decoder.decode("PackageServiceClient#getPackageById(Long)", response(400));

        assertThat(result).isInstanceOf(FeignException.class);
    }
}
