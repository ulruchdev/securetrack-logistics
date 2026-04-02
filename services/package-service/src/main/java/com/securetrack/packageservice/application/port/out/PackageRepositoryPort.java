package com.securetrack.packageservice.application.port.out;

import com.securetrack.packageservice.domain.model.Package;

public interface PackageRepositoryPort {
    Package save(Package packageDomain);
}