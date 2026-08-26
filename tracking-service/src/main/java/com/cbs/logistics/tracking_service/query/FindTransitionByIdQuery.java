package com.cbs.logistics.tracking_service.query;

/**
 * Query de lecture : "montre-moi la transition portant cet identifiant".
 */
public record FindTransitionByIdQuery(Long trackingId) {
}
