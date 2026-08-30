package com.cbs.logistics.tracking_service.api;

import com.cbs.logistics.tracking_service.query.FindHistoryQuery;
import com.cbs.logistics.tracking_service.query.FindTransitionByIdQuery;
import com.cbs.logistics.tracking_service.query.TransitionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Point d'entrée HTTP du flux de LECTURE.
 *
 * <p>Miroir du TrackingController : aucune logique, juste la traduction d'un
 * appel HTTP en Query et la publication du résultat via le QueryGateway.</p>
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking (Lecture)", description = "Consultation de l'historique des transitions (CQRS cote lecture)")
public class TrackingQueryController {

    /**
     * QueryGateway : façade Axon sur le QueryBus. Contrairement au
     * CommandGateway, il retourne des CompletableFuture — Axon est pensé
     * async par défaut côté lecture.
     */
    private final QueryGateway queryGateway;

    /**
     * ResponseTypes.multipleInstancesOf(TransitionDto.class) : indique au
     * QueryBus le type attendu en réponse. C'est ce qui permet à Axon de
     * sérialiser/désérialiser proprement (et, plus tard dans une archi
     * distribuée, de router vers le bon service).
     */
    @Operation(summary = "Historique d'un colis", description = "Liste chronologique de toutes les transitions de statut pour un colis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des transitions"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    @GetMapping("/package/{packageId}")
    public CompletableFuture<List<TransitionDto>> history(@PathVariable String packageId) {
        return queryGateway.query(
                new FindHistoryQuery(packageId),
                ResponseTypes.multipleInstancesOf(TransitionDto.class));
    }

    @Operation(summary = "Consulter une transition", description = "Recuperer les details d'une transition specifique par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transition trouvee"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Transition introuvable")
    })
    @GetMapping("/{trackingId}")
    public CompletableFuture<TransitionDto> transition(@PathVariable Long trackingId) {
        return queryGateway.query(
                new FindTransitionByIdQuery(trackingId),
                ResponseTypes.instanceOf(TransitionDto.class));
    }
}
