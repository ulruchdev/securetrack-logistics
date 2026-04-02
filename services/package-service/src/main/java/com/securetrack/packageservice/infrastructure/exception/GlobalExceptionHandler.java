package com.securetrack.packageservice.infrastructure.exception;

import com.securetrack.packageservice.presentation.dto.api.ErrorCode;
import com.securetrack.packageservice.presentation.dto.api.ProblemDetailResponse;
import com.securetrack.packageservice.presentation.dto.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, Object> additionalData = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                additionalData.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        });

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(ErrorCode.VALIDATION_ERROR.getType())
                .title(ErrorCode.VALIDATION_ERROR.getTitle())
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ErrorCode.VALIDATION_ERROR.getDefaultDetail())
                .instance(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .traceId(UUID.randomUUID().toString())
                .path(request.getRequestURI())
                .additionalData(additionalData)
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.error(problem));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        logger.error("Unhandled exception in handleGeneralException", ex);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(ErrorCode.INTERNAL_SERVER_ERROR.getType())
                .title(ErrorCode.INTERNAL_SERVER_ERROR.getTitle())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultDetail())
                .instance(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .traceId(UUID.randomUUID().toString())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.internalServerError().body(ApiResponse.error(problem));
    }
}