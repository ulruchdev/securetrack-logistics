package com.cbs.logistics.tracking_service.exception;

/**
 * Exception métier levée par {@code TrackingAggregate} quand une transition
 * viole une règle de gestion (colis déjà livré, statut inconnu...).
 *
 * <p>Elle traverse le CommandGateway jusqu'au contrôleur, où elle est traduite
 * en réponse HTTP 409 par le GlobalExceptionHandler.</p>
 */
public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(String message) {
        super(message);
    }
}
