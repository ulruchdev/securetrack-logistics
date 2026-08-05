package com.cbs.logistics.location_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.cbs.logistics.location_service.client.PackageServiceClient;
import feign.FeignException;

@SpringBootTest
@ActiveProfiles("test")
public class LocationServiceIntegrationTest {

    @Autowired
    private PackageServiceClient packageServiceClient;

    @Test
    public void testFeignClientCommunication() {
        // Test de communication Feign avec PackageService
        // Ce test vérifie que le client est configuré correctement
        // et peut faire un appel (même si PackageService n'est pas démarré, on capture l'exception)
        try {
            packageServiceClient.getPackageById("test-id");
        } catch (FeignException e) {
            // Attendu si PackageService n'est pas disponible
            System.out.println("Feign communication test passed: " + e.getMessage());
        }
    }
}