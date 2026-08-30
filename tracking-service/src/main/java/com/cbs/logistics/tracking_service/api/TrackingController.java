package com.cbs.logistics.tracking_service.api;

import com.cbs.logistics.tracking_service.command.RegisterTransitionCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Point d'entrée HTTP du flux d'ÉCRITURE.
 *
 * <p>Le contrôleur ne contient AUCUNE logique métier : il traduit une requête
 * HTTP en commande et la confie au CommandGateway. Toute la décision est dans
 * l'aggregate. C'est la séparation CQRS côté écriture.</p>
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking (Ecriture)", description = "Enregistrement des transitions de statut (CQRS cote ecriture)")
public class TrackingController {

    /**
     * CommandGateway : façade Axon sur le CommandBus. sendAndWait() :
     *  - route la commande vers le bon aggregate (via @TargetAggregateIdentifier) ;
     *  - ATTEND le résultat (synchrone) ;
     *  - propage les exceptions métier (InvalidTransitionException) telles
     *    quelles jusqu'ici -> traduites en HTTP par GlobalExceptionHandler.
     */
    private final CommandGateway commandGateway;

    @Operation(summary = "Enregistrer une transition", description = "Enregistrer un changement de statut pour un colis. Valide par l'aggregate (invariants metier).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transition enregistree"),
            @ApiResponse(responseCode = "400", description = "Erreur de validation ou JSON invalide"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "409", description = "Transition invalide (regle metier violee)")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> registerTransition(
            @Valid @RequestBody RegisterTransitionRequest request) {

        commandGateway.sendAndWait(new RegisterTransitionCommand(
                request.packageId(),
                request.locationId(),
                request.newStatus()
        ));

        // 201 : à ce stade, l'événement EST persisté dans l'event store
        // (cohérence forte garantie pour l'écriture).
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "packageId", request.packageId(),
                "status", request.newStatus()
        ));
    }
}
