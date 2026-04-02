package com.securetrack.packageservice.application.port.in;

import com.securetrack.packageservice.application.dto.command.CreatePackageCommand;
import com.securetrack.packageservice.application.dto.result.PackageResult;

public interface CreatePackageUseCase {
    PackageResult createPackage(CreatePackageCommand command);
}