package com.cbs.logistics.package_service.e2e;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.PackageStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test bout-en-bout avec des vrais conteneurs Docker (PostgreSQL + RabbitMQ).
 *
 * <p>Pour exécuter ce test en local :</p>
 * <pre>
 *   mvn test -pl package-service -Dtest=PackageServiceE2ETest \
 *       -Dsurefire.failIfNoSpecifiedTests=false
 * </pre>
 *
 * <p>Le tag "e2e" permet de l'exclure du build CI par défaut.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PackageServiceE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("cbsdb")
            .withUsername("cbsuser")
            .withPassword("cbspassword");

    @Container
    static GenericContainer<?> rabbitmq = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management"))
            .withExposedPorts(5672, 15672)
            .withEnv("RABBITMQ_DEFAULT_USER", "cbsuser")
            .withEnv("RABBITMQ_DEFAULT_PASS", "cbspassword");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "cbsuser");
        registry.add("spring.rabbitmq.password", () -> "cbspassword");
        // Désactiver le polling outbox en test
        registry.add("outbox.poll-interval-ms", () -> "999999");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private static String trackingNumber;
    private static Long packageId;

    @Test
    @Order(1)
    @DisplayName("POST /api/packages - Creer un colis, trackingNumber genere")
    void createPackage() {
        CreatePackageRequest request = new CreatePackageRequest();
        request.setPackageName("Colis E2E");
        request.setDescription("Test bout-en-bout avec Testcontainers");
        request.setPackageType("FRAGILE");
        request.setWeight(2.5);

        HttpEntity<CreatePackageRequest> entity = new HttpEntity<>(request, headers());

        ResponseEntity<PackageDto> response = restTemplate.exchange(
                "/api/packages", HttpMethod.POST, entity, PackageDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().trackingNumber().startsWith("ST-"));
        assertEquals(PackageStatus.NEW.name(), response.getBody().packageStatus());

        trackingNumber = response.getBody().trackingNumber();
        packageId = response.getBody().packageId();
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/packages/by-tracking/{trackingNumber} - Recherche par tracking")
    void getByTrackingNumber() {
        ResponseEntity<PackageDto> response = restTemplate.exchange(
                "/api/packages/by-tracking/" + trackingNumber,
                HttpMethod.GET, new HttpEntity<>(headers()), PackageDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Colis E2E", response.getBody().packageName());
        assertEquals(trackingNumber, response.getBody().trackingNumber());
    }

    @Test
    @Order(3)
    @DisplayName("PATCH /api/packages/{id} - Mise a jour partielle (statut uniquement)")
    void updatePackage() {
        UpdatePackageRequest update = new UpdatePackageRequest();
        update.setPackageStatus(PackageStatus.IN_TRANSIT);

        HttpEntity<UpdatePackageRequest> entity = new HttpEntity<>(update, headers());

        ResponseEntity<PackageDto> response = restTemplate.exchange(
                "/api/packages/" + packageId, HttpMethod.PATCH, entity, PackageDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PackageStatus.IN_TRANSIT.name(), response.getBody().packageStatus());
        assertEquals("Colis E2E", response.getBody().packageName());
    }

    @Test
    @Order(4)
    @DisplayName("DELETE /api/packages/{id} - Soft delete, 404 ensuite")
    void softDelete() {
        ResponseEntity<Void> del = restTemplate.exchange(
                "/api/packages/" + packageId, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, del.getStatusCode());

        ResponseEntity<String> get = restTemplate.exchange(
                "/api/packages/" + packageId, HttpMethod.GET,
                new HttpEntity<>(headers()), String.class);
        assertEquals(HttpStatus.NOT_FOUND, get.getStatusCode());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/packages - Colis supprime absent de la liste")
    void deletedNotInList() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/packages", HttpMethod.GET,
                new HttpEntity<>(headers()), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().contains(trackingNumber));
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Tenant-Id", "tenant-cbs-001");
        return h;
    }
}
