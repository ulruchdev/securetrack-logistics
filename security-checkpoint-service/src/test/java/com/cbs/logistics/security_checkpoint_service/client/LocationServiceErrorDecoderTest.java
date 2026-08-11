package com.cbs.logistics.security_checkpoint_service.client;

import com.cbs.logistics.security_checkpoint_service.exception.LocationNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.LocationServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de traduction des erreurs HTTP du Location Service
 * (le service ne doit jamais voir de types Feign - Clean Architecture).
 */
class LocationServiceErrorDecoderTest {

    private final LocationServiceErrorDecoder decoder = new LocationServiceErrorDecoder();

    private Response response(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://localhost:8082/api/locations/loc-1",
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
    void decode_shouldReturnLocationNotFound_when404() {
        Exception result = decoder.decode("LocationServiceClient#getLocationById(String)", response(404));

        assertThat(result)
                .isInstanceOf(LocationNotFoundException.class)
                .hasMessage("La localisation demandée n'existe pas");
    }

    @Test
    void decode_shouldReturnLocationServiceUnavailable_when500() {
        Exception result = decoder.decode("LocationServiceClient#getLocationById(String)", response(500));

        assertThat(result)
                .isInstanceOf(LocationServiceUnavailableException.class)
                .hasMessage("Le service de localisation est indisponible");
    }

    @Test
    void decode_shouldReturnLocationServiceUnavailable_when503() {
        Exception result = decoder.decode("LocationServiceClient#getLocationById(String)", response(503));

        assertThat(result).isInstanceOf(LocationServiceUnavailableException.class);
    }

    @Test
    void decode_shouldDelegateToDefaultDecoder_forOtherStatus() {
        Exception result = decoder.decode("LocationServiceClient#getLocationById(String)", response(400));

        assertThat(result).isInstanceOf(FeignException.class);
    }
}
