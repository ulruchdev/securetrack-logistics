package com.cbs.logistics.tracking_service.command;

import com.cbs.logistics.tracking_service.event.TrackingTransitionedEvent;
import com.cbs.logistics.tracking_service.exception.InvalidTransitionException;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.Set;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * L'AGRÉGAT — le gardien des règles métier du flux d'écriture.
 *
 * <p>Rôle : recevoir une commande, DÉCIDER si elle est valide et, si oui,
 * publier un événement. Il ne fait AUCUNE écriture en base : sa seule sortie
 * possible est {@code apply(unEvent)}. C'est l'event store qui persiste.</p>
 */
@Aggregate
public class TrackingAggregate {

    /** Statuts métier autorisés pour une transition. */
    public static final Set<String> ALLOWED_STATUSES =
            Set.of("NEW", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED");

    /**
     * Identifiant de l'aggregate. Axon l'utilise pour :
     *  - router les commandes (@TargetAggregateIdentifier doit correspondre) ;
     *  - indexer les événements dans l'event store ;
     *  - reconstruire l'état en rejouant les événements de CE packageId.
     */
    @AggregateIdentifier
    private String packageId;

    /** État courant (en mémoire), reconstruit par replay des événements. */
    private String currentStatus;

    /**
     * Constructeur vide OBLIGATOIRE (protected) : Axon instancie la classe par
     * réflexion avant d'invoquer le handler (création) ou de rejouer les
     * événements (reconstruction).
     */
    protected TrackingAggregate() {
        // requis par Axon
    }

    /**
     * Handler UNIQUE de la commande RegisterTransitionCommand.
     *
     * <p>Deux annotations combinées :</p>
     * <ul>
     *   <li>{@code @CommandHandler} : cette méthode traite la commande ;</li>
     *   <li>{@code @CreationPolicy(CREATE_IF_MISSING)} : si aucun aggregate
     *       n'existe encore pour ce packageId, Axon crée une instance vide
     *       (constructeur ci-dessus) puis invoque cette méthode ; sinon il
     *       charge l'aggregate existant en rejouant ses événements.</li>
     * </ul>
     *
     * <p>Dans les deux cas, la logique est identique : valider l'invariant,
     * puis appliquer un événement.</p>
     */
    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(RegisterTransitionCommand command) {
        validateStatus(command.newStatus());

        // INVARIANT : un colis livré est clos, définitivement.
        // (currentStatus == null signifie "première transition", donc pas d'invariant)
        if ("DELIVERED".equals(currentStatus)) {
            throw new InvalidTransitionException(
                    "Le colis " + packageId + " est déjà DELIVERED : aucune nouvelle transition n'est autorisée");
        }

        apply(new TrackingTransitionedEvent(
                command.packageId(),
                command.locationId(),
                command.newStatus(),
                Instant.now()
        ));
    }

    private void validateStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new InvalidTransitionException(
                    "Statut inconnu : " + status + ". Statuts autorisés : " + ALLOWED_STATUSES);
        }
    }

    /**
     * REJEU D'ÉVÉNEMENTS (event sourcing) : Axon appelle cette méthode pour
     * CHAQUE événement historique de cet aggregate afin de reconstruire l'état
     * interne. Elle ne fait AUCUNE validation et ne publie RIEN — elle se
     * contente de refléter le passé.
     *
     * <p>Point crucial : c'est elle qui positionne {@code packageId}, donc le
     * champ annoté {@code @AggregateIdentifier}. Sans ça, Axon ne saurait pas
     * à quel identifiant l'aggregate correspond après reconstruction.</p>
     */
    @EventSourcingHandler
    public void on(TrackingTransitionedEvent event) {
        this.packageId = event.packageId();
        this.currentStatus = event.newStatus();
    }
}
