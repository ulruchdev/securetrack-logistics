package com.securetrack.packageservice.presentation.mapper;

import com.securetrack.packageservice.application.dto.command.CreatePackageCommand;
import com.securetrack.packageservice.application.dto.result.PackageResult;
import com.securetrack.packageservice.presentation.dto.request.PackageCreateRequest;
import com.securetrack.packageservice.presentation.dto.response.PackageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PackagePresentationMapper {

    CreatePackageCommand toCommand(PackageCreateRequest request);

    PackageResponse toResponse(PackageResult result);
}