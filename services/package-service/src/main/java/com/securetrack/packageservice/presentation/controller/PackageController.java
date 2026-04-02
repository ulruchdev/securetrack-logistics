package com.securetrack.packageservice.presentation.controller;

import com.securetrack.packageservice.application.port.in.CreatePackageUseCase;
import com.securetrack.packageservice.application.dto.command.CreatePackageCommand;
import com.securetrack.packageservice.application.dto.result.PackageResult;
import com.securetrack.packageservice.presentation.dto.api.ApiResponse;
import com.securetrack.packageservice.presentation.dto.request.PackageCreateRequest;
import com.securetrack.packageservice.presentation.dto.response.PackageResponse;
import com.securetrack.packageservice.presentation.mapper.PackagePresentationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class PackageController {

    private final CreatePackageUseCase createPackageUseCase;
    private final PackagePresentationMapper presentationMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<PackageResponse>> createPackage(
            @Valid @RequestBody PackageCreateRequest request) {

        CreatePackageCommand command = presentationMapper.toCommand(request);
        PackageResult result = createPackageUseCase.createPackage(command);
        PackageResponse response = presentationMapper.toResponse(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}