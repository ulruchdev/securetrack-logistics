package com.cbs.logistics.package_service.dto;

import com.cbs.logistics.package_service.entity.PackageStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests Bean Validation des DTOs : chaque contrainte doit produire
 * exactement le message français attendu.
 */
class PackageRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ===== CreatePackageRequest =====

    @Test
    void createRequest_shouldBeValid_whenAllFieldsPresent() {
        CreatePackageRequest request = validCreateRequest();

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void createRequest_shouldRejectBlankDescription() {
        CreatePackageRequest request = validCreateRequest();
        request.setDescription("");

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("description")
                        && v.getMessage().equals("La description est obligatoire"));
    }

    @Test
    void createRequest_shouldRejectBlankPackageName() {
        CreatePackageRequest request = validCreateRequest();
        request.setPackageName(null);

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("packageName")
                        && v.getMessage().equals("Le nom du colis est obligatoire"));
    }

    @Test
    void createRequest_shouldRejectBlankPackageType() {
        CreatePackageRequest request = validCreateRequest();
        request.setPackageType(" ");

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("packageType")
                        && v.getMessage().equals("Le type de colis est obligatoire"));
    }

    @Test
    void createRequest_shouldRejectNullWeight() {
        CreatePackageRequest request = validCreateRequest();
        request.setWeight(null);

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("weight")
                        && v.getMessage().equals("Le poids est obligatoire"));
    }

    @Test
    void createRequest_shouldRejectNegativeWeight() {
        CreatePackageRequest request = validCreateRequest();
        request.setWeight(-1.0);

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("weight")
                        && v.getMessage().equals("Le poids doit être positif ou nul"));
    }

    @Test
    void createRequest_shouldRejectNullFragile() {
        CreatePackageRequest request = validCreateRequest();
        request.setFragile(null);

        Set<ConstraintViolation<CreatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("fragile")
                        && v.getMessage().equals("L'indicateur de fragilité est obligatoire"));
    }

    // ===== UpdatePackageRequest =====

    @Test
    void updateRequest_shouldBeValid_whenEmpty() {
        // PATCH partiel : aucun champ n'est obligatoire
        UpdatePackageRequest request = new UpdatePackageRequest();

        Set<ConstraintViolation<UpdatePackageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void updateRequest_shouldBeValid_whenStatusOnly() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(PackageStatus.IN_TRANSIT);

        Set<ConstraintViolation<UpdatePackageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void updateRequest_shouldRejectNegativeWeight() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setWeight(-5.0);

        Set<ConstraintViolation<UpdatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("weight")
                        && v.getMessage().equals("Le poids doit être positif ou nul"));
    }

    @Test
    void updateRequest_shouldRejectBlankDescription() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setDescription("");

        Set<ConstraintViolation<UpdatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("description")
                        && v.getMessage().equals("La description ne peut pas être vide"));
    }

    @Test
    void updateRequest_shouldRejectBlankPackageName() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageName(" ");

        Set<ConstraintViolation<UpdatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("packageName")
                        && v.getMessage().equals("Le nom du colis ne peut pas être vide"));
    }

    @Test
    void updateRequest_shouldRejectBlankPackageType() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageType("");

        Set<ConstraintViolation<UpdatePackageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("packageType")
                        && v.getMessage().equals("Le type de colis ne peut pas être vide"));
    }

    private CreatePackageRequest validCreateRequest() {
        CreatePackageRequest request = new CreatePackageRequest();
        request.setDescription("Colis fragile");
        request.setPackageName("Colis test");
        request.setPackageType("STANDARD");
        request.setWeight(2.5);
        request.setFragile(true);
        return request;
    }
}
