package com.cbs.logistics.tracking_service.config;

import org.axonframework.config.EventProcessingConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du mode de traitement des événements.
 *
 * <p>Par défaut, Axon utilise un processeur TRACKING : la projection est
 * mise à jour ASYNCHRONEMENT (un thread séparé lit l'event store à son
 * rythme, sa position étant suivie dans un "token"). C'est plus scalable,
 * mais introduit la cohérence éventuelle : un GET juste après un POST peut
 * ne rien voir pendant quelques millisecondes.</p>
 *
 * <p>Ici on bascule en SUBSCRIBING : la projection est mise à jour dans le
 * MÊME thread (et la même transaction) que la commande. Conséquences :</p>
 * <ul>
 *   <li>+ read-your-writes garanti : le GET qui suit immédiatement le POST
 *       voit toujours la transition ;</li>
 *   <li>- pas de scalabilité indépendante de la lecture (acceptable pour ce
 *       service mono-instance) ;</li>
 *   <li>- si la projection échoue, l'écriture échoue aussi (couplage).</li>
 * </ul>
 *
 * <p>Point de cours : c'est LE levier concret pour choisir où on place le
 * curseur entre "cohérence forte" et "performance" — une ligne de config.</p>
 */
@Configuration
public class EventProcessorConfig {

    @Autowired
    public void configureEventProcessors(EventProcessingConfigurer configurer) {
        // Tous les @ProcessingGroup passent en mode subscribing (synchrone).
        configurer.usingSubscribingEventProcessors();
    }
}
