# SecureTrack Logistics – Backlog Ultra-Granulaire
**Version :** 1.2  
**Date :** 29 mars 2026  
**DoD pour chaque tâche :** Code propre, compilé, test local OK, commit avec Conventional Commits, aucune tâche laissée en "CRUD entier".

## EPIC STL-1 – Core Logistics Engine (Semaines 1-2)

### US STL-001 – Enregistrer un nouveau colis

**Tâches (chacune réalisable en < 60 minutes) :**
- [ ] Créer l'enum PackageStatus.java (CREATED, IN_TRANSIT, AT_CHECKPOINT, DELIVERED, BLOCKED)
- [ ] Créer l'entity Package.java (@Entity, @Id, Lombok, tous les champs)
- [ ] Créer l'interface PackageRepository.java (extends JpaRepository)
- [ ] Créer le DTO PackageCreateRequest.java avec Bean Validation complète
- [ ] Créer le DTO PackageResponse.java
- [ ] Créer l'interface PackageMapper.java (MapStruct)
- [ ] Implémenter la méthode create dans PackageService.java (@Service + @Transactional)
- [ ] Implémenter le endpoint POST /api/v1/packages dans PackageController.java
- [ ] Gérer le retour 201 Created avec Location header
- [ ] Écrire le test unitaire PackageServiceTest.create_success (JUnit 5 + Mockito)
- [ ] Écrire le test unitaire PackageServiceTest.create_invalid_weight
- [ ] Créer le Dockerfile pour package-service
- [ ] Ajouter le service dans docker-compose.yml (port 8081)

### US STL-002 – Consulter un colis par ID
**Tâches :**
- [ ] Ajouter findById dans PackageRepository
- [ ] Ajouter getById dans PackageService
- [ ] Implémenter GET /api/v1/packages/{id} dans PackageController
- [ ] Gérer l'exception 404 avec ProblemDetail (errorCode = PKG-404)
- [ ] Écrire test unitaire getById_success
- [ ] Écrire test unitaire getById_notFound

### US STL-003 – Lister tous les colis avec pagination
**Tâches :** (découpées de la même façon : repository, service, controller, tests, etc.)

### US STL-004 – Mettre à jour un colis
### US STL-005 – Supprimer un colis (soft delete)

## EPIC STL-2 – Locations & Checkpoints (LocationService + SecurityCheckpointService)
## EPIC STL-3 – Security & Governance
## EPIC STL-4 – Real-time Intelligence (TrackingService + Axon)

**Labels Jira :** backend, database, docker, testing, security, documentation  
**Components :** PackageService, LocationService, SecurityCheckpointService, TrackingService