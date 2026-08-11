package com.cbs.logistics.location_service.client;

import com.cbs.logistics.location_service.exception.PackageNotFoundException;
import com.cbs.logistics.location_service.exception.PackageServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Traduit les erreurs HTTP du Package Service en exceptions de domaine,
 * afin que la couche service ne dépende pas des types Feign.
 */
@Slf4j
@Component
public class PackageServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            log.warn("Colis introuvable chez Package Service : {}", response.request().url());
            return new PackageNotFoundException("Le colis demandé n'existe pas");
        }
        if (response.status() >= 500) {
            log.error("Package Service en erreur (status {}) : {}", response.status(), response.request().url());
            return new PackageServiceUnavailableException("Le service de colis est indisponible");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
