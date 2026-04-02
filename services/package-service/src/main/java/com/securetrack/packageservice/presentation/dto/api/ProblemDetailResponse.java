package com.securetrack.packageservice.presentation.dto.api;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemDetailResponse {

    private String type;           // URI du type d'erreur (ex: https://securetrack.com/errors/pkg-001)
    private String title;
    private int status;
    private String detail;
    private String instance;       // Chemin de l'API qui a échoué
    private LocalDateTime timestamp;
    private String errorCode;      // Code métier (PKG-001, etc.)
    private String traceId;        // Pour le suivi dans les logs
    private String correlationId;  // Optionnel : identifiant de corrélation
    private String path;           // Chemin de la requête
    private Map<String, Object> additionalData;

    public static ProblemDetailResponseBuilder builder() {
        return new ProblemDetailResponseBuilder();
    }
}