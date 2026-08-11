package com.cbs.logistics.security_checkpoint_service.client;

import com.cbs.logistics.security_checkpoint_service.exception.LocationNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.LocationServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Traduit les erreurs HTTP du Location Service en exceptions de domaine,
 * afin que la couche service ne dépende pas des types Feign.
 */
@Slf4j
@Component
public class LocationServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            log.warn("Localisation introuvable chez Location Service : {}", response.request().url());
            return new LocationNotFoundException("La localisation demandée n'existe pas");
        }
        if (response.status() >= 500) {
            log.error("Location Service en erreur (status {}) : {}", response.status(), response.request().url());
            return new LocationServiceUnavailableException("Le service de localisation est indisponible");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
