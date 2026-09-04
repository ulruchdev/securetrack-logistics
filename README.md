# CBS Onboarding - Projet 1 : Logistique

Système de microservices pour la gestion de la chaîne logistique CBS : suivi des colis, localisation des points de transition et enregistrement sécurisé des passages aux checkpoints.

## Architecture

| Service | Port | Base de données | Rôle |
|---|---|---|---|
| **package-service** | 8081 | PostgreSQL (`cbsdb`) | CRUD des colis (soft delete, transactional outbox → RabbitMQ) |
| **location-service** | 8082 | MongoDB + PostgreSQL | Localisation des colis (Mongo) + Sites/Checkpoints (SQL) |
| **security-checkpoint-service** | 8083 | PostgreSQL (`cbsdb`) | Logs de passages sécurisés — valide la localisation via Location Service |
| **tracking-service** | 8084 | PostgreSQL (`cbsdb`) | Historique des transitions de statut — **CQRS + Event Sourcing** (Axon Framework) |

### Infrastructure

| Composant | Port | Rôle |
|---|---|---|
| PostgreSQL | 5433 | Base SQL partagée (packages, checkpoints, tracking, sites) |
| MongoDB | 27017 | Base NoSQL (localisations) |
| Redis | 6379 | Cache d'indisponibilité des lieux |
| RabbitMQ | 5672 (15672 mgmt) | Broker événementiel — Package → Tracking (status changes) |

### Patterns architecturaux

- **Clean Architecture / DDD** : domain layer isolé, ports & adapters
- **JWT Resource Server** : authentification centralisée via `common-security`
- **Multi-tenant** : `tenant_id` sur chaque entité, extraction depuis le JWT
- **Transactional Outbox** : garantie de livraison des événements RabbitMQ (retry 5x → DLQ)
- **Soft Delete** : `deleted_at` sur Package, filtrage transparent
- **RFC 7807 ProblemDetail** : format d'erreur unifié sur les 4 services

Communication inter-services : **OpenFeign** (Package Service ← Location Service ← Security Checkpoint Service). Le Tracking Service reçoit les événements de statut via **RabbitMQ** et utilise **Axon Framework** (CQRS/Event Sourcing) pour reconstruire l'historique.

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Docker + Docker Compose** (PostgreSQL, MongoDB, Redis, RabbitMQ)

## Démarrage rapide

### 1. Infrastructure (bases de données)

```bash
cd common
cp .env.example .env      # personnaliser les identifiants si besoin (fichier non versionné)
docker compose up -d
```

Les bases sont exposées **uniquement en local** (`127.0.0.1`) : PostgreSQL sur le port **5433**, MongoDB sur **27017**, Redis sur **6379**, RabbitMQ sur **5672** (mgmt **15672**).

### 2. Variables d'environnement des services (OBLIGATOIRES)

> 🔑 Les mots de passe n'ont **plus de valeur par défaut** dans le code ni dans les `application.yml`.
> Un service **refuse de démarrer** si sa variable n'est pas définie.

```bash
# ⚠️ DB_PASSWORD et MONGO_URI doivent correspondre aux identifiants de l'infrastructure
#    définis dans common/.env (étape 1).
export DB_PASSWORD="<votre-mot-de-passe>"              # identique à POSTGRES_PASSWORD de common/.env
export MONGO_URI="mongodb://cbsuser:<votre-mot-de-passe>@localhost:27017/cbsdb?authSource=admin"
export JWT_SECRET="<clé-base64-≥32-bytes>"             # clé HMAC pour JWT (dev only)
export SECURITY_PASSWORD="<mot-de-passe-admin>"        # Basic Auth interne (dev only)
```

| Variable | Obligatoire | Sert à | Correspondance (`common/.env`) |
|---|---|---|---|
| `DB_PASSWORD` | ✅ | Mot de passe PostgreSQL | `POSTGRES_PASSWORD` |
| `MONGO_URI` | ✅ | URI MongoDB (location-service) | `MONGO_USER:MONGO_PASSWORD` |
| `JWT_SECRET` | ✅ | Clé HMAC JWT (base64, ≥32 bytes) | — |
| `SECURITY_PASSWORD` | ✅ | Basic Auth interne (dev) | — |
| `RABBITMQ_PASSWORD` | ✅ | Mot de passe RabbitMQ | `RABBITMQ_PASSWORD` dans `.env` |

### 3. Services

Depuis la racine du dépôt (après avoir exporté les variables ci-dessus) :

```bash
# Package Service (port 8081)
cd package-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Location Service (port 8082)
cd location-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Security Checkpoint Service (port 8083)
cd security-checkpoint-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Tracking Service (port 8084) — CQRS + Event Sourcing ( nécessite RabbitMQ )
cd tracking-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> 💡 **Schéma de base de données** : géré par **Liquibase** (migrations versionnées dans `db/changelog/`). `ddl-auto` est en `none` dans tous les profils — chaque évolution passe par un changeset Liquibase.

## Configuration & sécurité

- **Secrets obligatoires, jamais versionnés** : les mots de passe n'ont **plus de valeur par défaut** — lus depuis l'environnement. Un service **refuse de démarrer** si sa variable est absente. Liste dans [`common/.env.example`](common/.env.example).
- **JWT Resource Server** : les 4 services valident un JWT HMAC partagé (`common-security`). En prod, configurez `jwt.jwk-set-uri` vers votre IdP.
- **Multi-tenant** : chaque entité porte un `tenant_id` extrait du claim JWT. Isolation testée.
- **HTTPS** : exigé **par défaut** (désactivé en profil `dev`).
- **Feign JWT Relay** : les appels inter-services forwardent automatiquement le token JWT.

## Flux principal

```
1. Créer un Site (location-service) → POST /api/sites
2. Créer un Checkpoint sur le Site → POST /api/sites/{siteId}/checkpoints
3. Créer un Colis (package-service) → POST /api/packages → reçoit trackingNumber (ST-XXXXXXXX)
4. Enregistrer un scan au checkpoint → POST /api/checkpoints/scan → enregistre le passage
5. Le Package Service publie un événement de changement de statut (outbox → RabbitMQ)
6. Le Tracking Service consomme l'événement et enregistre la transition (Event Sourcing)
```

## Tests

```bash
mvn clean verify
```

> `mvn verify` exécute les tests **et** le contrôle de couverture JaCoCo (seuil minimal 80 %) pour les 5 modules.

## Documentation API (Swagger UI)

> ⚠️ Swagger UI est protégé par JWT en production. En dev, il est accessible sans authentification.

- Package Service : `http://localhost:8081/swagger-ui/index.html`
- Location Service : `http://localhost:8082/swagger-ui/index.html`
- Security Checkpoint Service : `http://localhost:8083/swagger-ui/index.html`
- Tracking Service : `http://localhost:8084/swagger-ui/index.html`
- **Specs OpenAPI** : `docs/combined-openapi.yaml` (vue combinée) + une spec dédiée par service.
- Les erreurs suivent **RFC 7807** (`application/problem+json`).

## Structure du dépôt

```
├── parent/                          # POM parent commun
├── common-dto/                      # DTOs partagés (PackageDto, events)
├── common-security/                 # JWT Resource Server + TenantFilter partagé
├── package-service/                 # CRUD colis + outbox events
├── location-service/                # Localisation (Mongo) + Sites/Checkpoints (SQL)
├── security-checkpoint-service/     # Logs de scan sécurisés
├── tracking-service/                # CQRS + Event Sourcing (Axon)
├── docs/                            # OpenAPI specs + script de génération
├── scripts/api-tests/               # Scripts de tests API (bash)
├── common/                          # Docker Compose + .env.example
└── docker-compose.yml               # Infrastructure complète
```

## Documentation produit

Vision, correctifs backend et architecture frontend (web + mobile) :

- [`docs/produit/README.md`](docs/produit/README.md) — sur la branche `docs-produit`
