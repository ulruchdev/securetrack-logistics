package com.cbs.logistics.package_service.exception;

public class PackageNotFoundException extends RuntimeException {
    public PackageNotFoundException(String message) {
        super(message);
    }

    public PackageNotFoundException(Long id) {
        super("Package not found with id: " + id);
    }
}