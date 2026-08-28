package com.cbs.logistics.tracking_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Gestion des erreurs conforme au contrat commun du projet :
 * ProblemDetail RFC 7807, application/problem+json.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Invariant métier violé (colis déjà livré, statut inconnu) -> 409 CONFLICT. */
    @ExceptionHandler(InvalidTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Transition invalide");
        return problem;
    }

    /** Ressource de lecture introuvable -> 404 NOT_FOUND. */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Ressource introuvable");
        return problem;
    }

    /** Erreurs de validation Bean Validation (@NotBlank) -> 400 BAD_REQUEST. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La requête est invalide");
        problem.setTitle("Erreur de validation");
        problem.setProperty("fieldErrors", ex.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", f.getDefaultMessage()))
                .toList());
        return problem;
    }

    /** Paramètre de path/query avec un type invalide -> 400 BAD_REQUEST. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Le paramètre '" + ex.getName() + "' a une valeur invalide : '" + ex.getValue() + "'");
        problem.setTitle("Paramètre invalide");
        return problem;
    }

    /** Corps de requête JSON malformé ou illisible -> 400 BAD_REQUEST. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Corps de requête invalide ou illisible");
        problem.setTitle("Requête invalide");
        return problem;
    }

    /** Tout le reste : erreur technique imprévue -> 500, message générique
     *  (jamais le stacktrace ni e.getMessage() au client). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
        problem.setTitle("Erreur interne du serveur");
        return problem;
    }
}
