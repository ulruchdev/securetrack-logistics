package com.cbs.logistics.security_checkpoint_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Point d'entrée d'authentification : transforme un échec d'authentification
 * (identifiants absents ou invalides) en réponse RFC 7807 (application/problem+json),
 * tout en conservant le header WWW-Authenticate attendu par le Basic Auth.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentification requise : identifiants absents ou invalides");
        problemDetail.setTitle("Non autorisé");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Conservé pour la compatibilité Basic Auth (le navigateur l'affiche s'il veut)
        response.setHeader("WWW-Authenticate", "Basic realm=\"CBS Logistics\"");

        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
