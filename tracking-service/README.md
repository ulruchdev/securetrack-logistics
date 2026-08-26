# CBS Logistics - Tracking Service

## Description métier

Le Tracking Service est un microservice RESTful responsable de l'enregistrement et de la consultation de l'historique des transitions de statut des colis. Il implémente les patterns **CQRS** (Command Query Responsibility Segregation) et **Event Sourcing** via **Axon Framework** : chaque changement de statut est enregistré comme un événement immuable, permettant de reconstruire l'état complet d'un colis à tout moment.

**Principe fondamental** : il n'y a pas de table de « l'état actuel » — l'état est **dérivé** de l'historique des événements. Chaque écriture est un événement passé, chaque lecture est une projection reconstituée.

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Docker et Docker Compose** pour la base de données PostgreSQL (utilise le `docker-compose.yml` commun)

## Architecture CQRS / Event Sourcing

```
                         ┌─────────────────────────────────────────┐
                         │            Tracking Service              │
                         │                                         │
  POST /api/tracking ──► │  TrackingController                     │
                         │    └─ CommandGateway.sendAndWait()      │
                         │         └─ TrackingAggregate            │
                         │              └─ apply(Event)            │
                         │                   └─ Event Store (JPA)  │
                         │                        └─ @EventHandler │
                         │                             └─ Projection│
                         │                                  └─ DB  │
  GET  /api/tracking ──► │  TrackingQueryController                │
                         │    └─ QueryGateway.query()              │
                         │         └─ TrackingQueryHandler         │
                         │              └─ tracking_history        │
                         └─────────────────────────────────────────┘
```

| Couche | Responsabilité | Fichiers |
|---|---|---|
| **Command** (écriture) | Valider les invariants, publier des événements | `RegisterTransitionCommand`, `TrackingAggregate` |
| **Event** | Faits passés, immuables | `TrackingTransitionedEvent` |
| **Query** (lecture) | Projections rapides pour le front | `TrackingQueryHandler`, `TrackingHistoryProjection` |
| **API** | Points d'entrée HTTP | `TrackingController` (POST), `TrackingQueryController` (GET) |

## Lancement du service

1. **Démarrer la base de données PostgreSQL :**
   ```bash
   cd common
   docker-compose up -d
   ```

2. **Configurer la variable d'environnement :**
   ```bash
   export DB_PASSWORD="<votre-mot-de-passe>"   # identique à POSTGRES_PASSWORD de common/.env
   ```

3. **Lancer l'application :**
   ```bash
   cd tracking-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

> 💡 **Schéma de base de données** : géré par **Liquibase** (table `tracking_history`) pour la projection de lecture. Les tables de l'event store Axon (`domain_event_entry`, `snapshot_event_entry`, `token_entry`) sont créées automatiquement par Hibernate via `ddl-auto: update`.

> 💡 **Sérialisation** : le service utilise **Jackson** (`axon.serializer.general: jackson`) au lieu de XStream par défaut, compatible avec les records Java 21+.

L'application sera accessible sur `http://localhost:8084`.

## Endpoints API

### Enregistrer une transition (écriture)

```bash
curl -X POST http://localhost:8084/api/tracking \
  -H "Content-Type: application/json" \
  -d '{
    "packageId": "PKG-001",
    "locationId": 1,
    "newStatus": "NEW"
  }'
```

**Réponse** `201 Created` :
```json
{
  "packageId": "PKG-001",
  "status": "NEW"
}
```

**Statuts autorisés** : `NEW` → `IN_TRANSIT` → `DELIVERED` (terminal) ou `LOST` (terminal).

**Erreurs métier** :
- `409` — Transition invalide (ex: `DELIVERED` → `NEW`)
- `400` — Champs requis manquants (`packageId`, `newStatus`)

### Consulter l'historique d'un colis (lecture)

```bash
curl http://localhost:8084/api/tracking/package/PKG-001
```

**Réponse** `200 OK` :
```json
[
  {
    "trackingId": 1,
    "packageId": "PKG-001",
    "locationId": 1,
    "previousStatus": null,
    "newStatus": "NEW",
    "transitionDate": "2025-08-26T10:30:00Z"
  },
  {
    "trackingId": 2,
    "packageId": "PKG-001",
    "locationId": 2,
    "previousStatus": "NEW",
    "newStatus": "IN_TRANSIT",
    "transitionDate": "2025-08-26T11:15:00Z"
  }
]
```

### Consulter une transition par son identifiant

```bash
curl http://localhost:8084/api/tracking/1
```

**Réponse** `200 OK` : un objet `TransitionDto`.

**Erreur** :
- `404` — Transition introuvable (ProblemDetail RFC 7807).

## Gestion des erreurs

Toutes les erreurs suivent le standard **RFC 7807** (`application/problem+json`) :

```json
{
  "type": "about:blank",
  "title": "Transition invalide",
  "status": 409,
  "detail": "Statut inconnu : ARCHIVED. Statuts autorisés : [NEW, IN_TRANSIT, DELIVERED, LOST]",
  "instance": "/api/tracking"
}
```

| HTTP | Cause |
|---|---|
| `400` | Requête invalide (champs manquants, JSON malformé) |
| `404` | Transition introuvable |
| `409` | Règle métier violée (transition interdite) |
| `500` | Erreur interne inattendue |

## Configuration

| Propriété | Défaut | Description |
|---|---|---|
| `server.port` | `8084` | Port HTTP |
| `DB_URL` | `jdbc:postgresql://localhost:5433/cbsdb` | URL PostgreSQL |
| `DB_USERNAME` | `cbsuser` | Utilisateur PostgreSQL |
| `DB_PASSWORD` | — (obligatoire) | Mot de passe PostgreSQL |
| `axon.axonserver.enabled` | `false` | Désactive AxonServer (JPA event store local) |
| `axon.serializer.general` | `jackson` | Sérialiseur pour l'event store |

## Tests

```bash
cd tracking-service
mvn clean verify
```

> `mvn verify` exécute les tests **et** le contrôle de couverture JaCoCo (seuil minimal 80 %).

### Types de tests

| Classe | Ce qu'elle prouve |
|---|---|
| `TrackingAggregateTest` | Command → Event + invariants métier + rejeu (event sourcing) |
| `TrackingHistoryProjectionTest` | `@EventHandler` écrit correctement dans `tracking_history` |
| `TrackingQueryHandlerTest` | `@QueryHandler` retourne les bonnes données |
| `TrackingControllerTest` | POST → 201, 400, 409 via MockMvc |
| `TrackingQueryControllerTest` | GET → 200, 404 via MockMvc |

## Structure du module

```
tracking-service/
├── src/main/java/com/cbs/logistics/tracking_service/
│   ├── TrackingServiceApplication.java
│   ├── api/
│   │   ├── RegisterTransitionRequest.java    # DTO requête (record)
│   │   ├── TrackingController.java           # POST — écriture
│   │   └── TrackingQueryController.java      # GET — lecture
│   ├── command/
│   │   ├── RegisterTransitionCommand.java    # Commande Axon
│   │   └── TrackingAggregate.java            # Aggregate (cœur métier)
│   ├── event/
│   │   └── TrackingTransitionedEvent.java    # Événement immuable
│   ├── query/
│   │   ├── FindHistoryQuery.java
│   │   ├── FindTransitionByIdQuery.java
│   │   ├── TransitionDto.java
│   │   ├── TrackingHistoryEntry.java         # Entité JPA (projection)
│   │   ├── TrackingHistoryRepository.java
│   │   ├── TrackingHistoryProjection.java    # @EventHandler
│   │   └── TrackingQueryHandler.java         # @QueryHandler
│   ├── config/
│   │   └── EventProcessorConfig.java         # Subscribing processor
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── InvalidTransitionException.java
│       └── NotFoundException.java
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/
│       ├── db.changelog-master.yaml
│       └── 001-create-tracking-history.yaml
└── src/test/java/... (5 classes de test)
```

## Phase 2 — Intégration Event-Driven

### Contexte

En Phase 1, les transitions de statut sont enregistrées **manuellement** via `POST /api/tracking`.
En Phase 2, le TrackingService **écoute automatiquement** les événements de statut publiés
par le Package Service, garantissant la cohérence entre les deux services sans intervention humaine.

### Architecture cible

```
┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│  Package Service  │         │  Message Broker   │         │ Tracking Service  │
│                   │         │  (Kafka/RabbitMQ) │         │                   │
│  PATCH /packages  │         │                   │         │                   │
│  → update status  │──publish──►  topic:          │──consume──►  @EventHandler   │
│                   │         │  package-status   │         │  → projection     │
└──────────────────┘         └──────────────────┘         └──────────────────┘
```

### Événement métier : `PackageStatusChangedEvent`

| Champ | Type | Description |
|---|---|---|
| `packageId` | `Long` | Identifiant du colis |
| `previousStatus` | `PackageStatus` | Statut avant la transition |
| `newStatus` | `PackageStatus` | Statut après la transition |
| `locationId` | `Long` | Localisation (optionnel) |
| `timestamp` | `Instant` | Horodatage de la transition |

### Brokers envisagés

| Broker | Avantages | Inconvénients |
|---|---|---|
| **Apache Kafka** | Très haute performance, durabilité, replay | Infrastructure lourde, courbe d'apprentissage |
| **RabbitMQ** | Simple, routing flexible, bon pour microservices | Moins performant que Kafka pour gros volumes |
| **Spring ApplicationEvents** | Aucune infra supplémentaire | Mono-JVM, pas de persistance, pas de replay |

**Recommandation Phase 2** : `Spring ApplicationEvents` (option C) pour le MVP,
puis migration vers Kafka si besoin de scalabilité.

### Implémentation proposée (Spring ApplicationEvents)

#### 1. Package Service — Publication d'événements

```java
// package-service/.../event/PackageStatusChangedEvent.java
public record PackageStatusChangedEvent(
    Long packageId,
    PackageStatus previousStatus,
    PackageStatus newStatus,
    Long locationId,
    Instant timestamp
) {}

// package-service/.../service/PackageService.java
@Service
@RequiredArgsConstructor
public class PackageService {
    private final ApplicationEventPublisher eventPublisher;

    public PackageDto update(Long id, UpdatePackageRequest request) {
        Package entity = repository.findById(id).orElseThrow(...);
        PackageStatus previousStatus = entity.getPackageStatus();

        validateStatusTransition(previousStatus, request.getPackageStatus());
        mapper.updateEntityFromRequest(request, entity);
        Package saved = repository.save(entity);

        // Publication de l'événement
        eventPublisher.publishEvent(new PackageStatusChangedEvent(
            saved.getPackageId(),
            previousStatus,
            saved.getPackageStatus(),
            request.getLocationId(),
            Instant.now()
        ));

        return mapper.toDto(saved);
    }
}
```

#### 2. Tracking Service — Écoute des événements

```java
// tracking-service/.../event/PackageStatusChangedEventHandler.java
@Component
@RequiredArgsConstructor
public class PackageStatusChangedEventHandler {

    private final CommandGateway commandGateway;

    @EventListener
    public void onPackageStatusChanged(PackageStatusChangedEvent event) {
        // Conversion de l'événement métier → commande Axon
        commandGateway.sendAndWait(new RegisterTransitionCommand(
            String.valueOf(event.packageId()),
            event.locationId() != null ? String.valueOf(event.locationId()) : null,
            event.newStatus().name()
        ));
    }
}
```

#### 3. Dépendances à ajouter

| Service | Dépendance |
|---|---|
| Package Service | `spring-boot-starter` (déjà présent — `ApplicationEventPublisher` est inclus) |
| Tracking Service | Aucune — la classe `PackageStatusChangedEvent` doit être partagée via `common-dto` |

### Migration Kafka (Phase 2b)

Si le besoin de scalabilité se présente :

1. Ajouter `spring-kafka` aux deux services
2. Package Service : `KafkaTemplate.send("package-status", event)`
3. Tracking Service : `@KafkaListener(topics = "package-status")`
4. La classe `PackageStatusChangedEvent` reste identique (DTO partagé via `common-dto`)

### Avantages de l'event-driven

| Avantage | Impact |
|---|---|
| **Découplage** | Package Service ne connaît pas Tracking Service |
| **Cohérence** | Les transitions sont automatiques, pas manuelles |
| **Audit trail** | Les événements Kafka sont persistés et re-jouables |
| **Scalabilité** | Tracking Service peut être multi-instance (consommateur concurrent) |
| **Réversibilité** | En cas d'erreur, on peut re-jouer les événements depuis le topic |

### Priorité

| Critère | Évaluation |
|---|---|
| Valeur métier | 🟡 Moyenne — les appels manuels fonctionnent en dev/stage |
| Complexité | 🟢 Faible avec Spring Events, 🟠 Moyenne avec Kafka |
| Risque | 🟢 Faible — changement interne, pas d'impact API |
| Recommandation | **Phase 2 MVP** : Spring Events. **Phase 2b** : Kafka si production.
