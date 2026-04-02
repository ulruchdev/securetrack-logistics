package com.securetrack.packageservice.presentation.dto.api;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ProblemDetailResponse error;   // Changé en ProblemDetailResponse
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ProblemDetailResponse error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
}