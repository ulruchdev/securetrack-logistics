package com.cbs.logistics.tracking_service.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Commande d'écriture : intention impérative d'enregistrer une transition
 * de statut pour un colis.
 *
 * <p>Une commande est un ORDRE (peut échouer). Elle ne décrit pas un fait :
 * c'est l'aggregate qui décide si l'ordre est valide et qui publie alors
 * un événement au passé.</p>
 *
 * @param packageId  identifiant du colis = identifiant de l'aggregate
 * @param locationId lieu du checkpoint (optionnel)
 * @param newStatus  statut visé (NEW, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED)
 */
public record RegisterTransitionCommand(

        /*
         * @TargetAggregateIdentifier : dit à Axon que cette propriété sert à
         * ROUTER la commande vers la bonne instance d'aggregate. Quand la
         * commande arrive sur le CommandBus, Axon lit packageId, cherche
         * l'aggregate correspondant dans son repository et lui délègue.
         */
        @TargetAggregateIdentifier
        String packageId,

        String locationId,

        String newStatus
) {
}
