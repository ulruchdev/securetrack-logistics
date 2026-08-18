package com.cbs.logistics.security_checkpoint_service.exception;

/**
 * Levée quand la localisation récupérée auprès du Location Service est rattachée
 * à un colis différent de celui de la requête de checkpoint.
 */
public class LocationPackageMismatchException extends RuntimeException {

    public LocationPackageMismatchException(String locationId, Long requestPackageId, Long locationPackageId) {
        super("La localisation " + locationId + " est rattachée au colis " + locationPackageId
                + " alors que la requête concerne le colis " + requestPackageId);
    }
}
