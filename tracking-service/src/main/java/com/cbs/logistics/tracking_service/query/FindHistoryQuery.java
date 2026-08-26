package com.cbs.logistics.tracking_service.query;

/**
 * Query de lecture : "quel est l'historique complet des transitions du colis ?"
 * Contrairement à une commande, une query ne modifie rien et ne peut pas
 * échouer pour raison métier.
 */
public record FindHistoryQuery(String packageId) {
}
