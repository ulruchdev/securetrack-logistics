# CBS Onboarding - Projet 1 : Logistique

Systeme de microservices pour la gestion de la chaine logistique CBS : suivi des colis, localisation des points de transition et enregistrement securise des passages aux checkpoints.

## Architecture

| Service | Port | Base de donnees | Role |
|---|---|---|---|
| **gateway-service** | 8080 | — | API Gateway (Spring Cloud Gateway) — point d'entree unique JWT |
| **package-service** | 8081 | PostgreSQL (`cbsdb`) | CRUD des colis (soft delete, transactional outbox → RabbitMQ) |
| **location-service** | 8082 | MongoDB + PostgreSQL | Localisation des colis (Mongo) + Sites/Checkpoints (SQL) |
| **security-checkpoint-service** | 8083 | PostgreSQL (`cbsdb`) | Logs de passages securises — valide la localisation via Location Service |
| **tracking-service** | 8084 | PostgreSQL (`cbsdb`) | Historique des transitions de statut — **CQRS + Event Sourcing** (Axon Framework) |

### Infrastructure

| Composant | Port | Role |
|---|---|---|
| PostgreSQL | 5433 | Base SQL partagee (packages, checkpoints, tracking, sites) |
| MongoDB | 27017 | Base NoSQL (localisations) |
| Redis | 6379 | Cache d'indisponibilite des lieux |
| RabbitMQ | 5672 (15672 mgmt) | Broker evenementiel — Package → Tracking (status changes) |

### Patterns architecturaux

- **Clean Architecture / DDD** : domain layer isole, ports & adapters
- **JWT Resource Server** : authentification centralisee via `common-security`
- **Multi-tenant** : `tenant_id` sur chaque entite, extraction depuis le JWT
- **Transactional Outbox** : garantie de livraison des evenements RabbitMQ (retry 5x → DLQ)
- **Soft Delete** : `deleted_at` sur Package, filtrage transparent
- **RFC 7807 ProblemDetail** : format d'erreur unifie sur les 4 services
- **Spring Cloud Gateway** : point d'entree unique, routing JWT, Swagger via routes prefixees

Communication inter-services : **OpenFeign** (Package Service ← Location Service ← Security Checkpoint Service). Le Tracking Service recoit les evenements de statut via **RabbitMQ** et utilise **Axon Framework** (CQRS/Event Sourcing) pour reconstruire l'historique.

## Prerequis

- **Java 21** ou superieur
- **Maven 3.8+**
- **Docker + Docker Compose** (PostgreSQL, MongoDB, Redis, RabbitMQ)

## Demarrage rapide

### 1. Infrastructure (bases de donnees)

```bash
cd common
cp .env.example .env      # personnaliser les identifiants si besoin (fichier non versionne)
docker compose up -d
```

Les bases sont exposees **uniquement en local** (`127.0.0.1`) : PostgreSQL sur le port **5433**, MongoDB sur **27017**, Redis sur **6379**, RabbitMQ sur **5672** (mgmt **15672**).

### 2. Variables d'environnement des services (OBLIGATOIRES)

> Les mots de passe n'ont **plus de valeur par defaut** dans le code ni dans les `application.yml`.
> Un service **refuse de demarrer** si sa variable n'est pas definie.

```bash
# DB_PASSWORD et MONGO_URI doivent correspondre aux identifiants de l'infrastructure
# definis dans common/.env (etape 1).
export DB_PASSWORD="<votre-mot-de-passe>"              # identique a POSTGRES_PASSWORD de common/.env
export MONGO_URI="mongodb://cbsuser:<votre-mot-de-passe>@localhost:27017/cbsdb?authSource=admin"
export JWT_SECRET="<cle-base64-≥32-bytes>"             # cle HMAC pour JWT (dev only)
export SECURITY_PASSWORD="<mot-de-passe-admin>"        # Basic Auth interne (dev only)
export REDIS_PASSWORD="<mot-de-passe-redis>"
export RABBITMQ_PASSWORD="<mot-de-passe-rabbitmq>"
```

| Variable | Obligatoire | Sert a | Correspondance (`common/.env`) |
|---|---|---|---|
| `DB_PASSWORD` | ✅ | Mot de passe PostgreSQL | `POSTGRES_PASSWORD` |
| `MONGO_URI` | ✅ | URI MongoDB (location-service) | `MONGO_USER:MONGO_PASSWORD` |
| `JWT_SECRET` | ✅ | Cle HMAC JWT (base64, ≥32 bytes) | — |
| `SECURITY_PASSWORD` | ✅ | Basic Auth interne (dev) | — |
| `RABBITMQ_PASSWORD` | ✅ | Mot de passe RabbitMQ | `RABBITMQ_PASSWORD` dans `.env` |

### 3. Services

Depuis la racine du depot (apres avoir exporte les variables ci-dessus) :

```bash
# Gateway (port 8080) — point d'entree unique
cd gateway-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package Service (port 8081)
cd package-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Location Service (port 8082)
cd location-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Security Checkpoint Service (port 8083)
cd security-checkpoint-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Tracking Service (port 8084) — CQRS + Event Sourcing (necessite RabbitMQ)
cd tracking-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> **Ordre de demarrage** : Infrastructure → Package Service → Location Service → Security Checkpoint → Tracking → Gateway (dernier, car il route vers les 4).

> **Schema de base de donnees** : gere par **Liquibase** (migrations versionnees dans `db/changelog/`). `ddl-auto` est en `none` dans tous les profils — chaque evolution passe par un changeset Liquibase.

### 4. Docker Compose (tout-en-un)

```bash
# Depuis la racine du depot
cd common
cp .env.example .env
# Editer .env avec les vrais mots de passe

# Demarrer infrastructure + tous les services
docker compose --profile full up -d

# Verifier les healthchecks
docker compose ps
# Tous les services doivent etre "healthy"

# Arreter
docker compose --profile full down
```

## Configuration & securite

- **Secrets obligatoires, jamais versionnes** : les mots de passe n'ont **plus de valeur par defaut** — lus depuis l'environnement. Un service **refuse de demarrer** si sa variable est absente. Liste dans [`common/.env.example`](common/.env.example).
- **JWT Resource Server** : les 4 services valident un JWT HMAC partage (`common-security`). En prod, configurez `jwt.jwk-set-uri` vers votre IdP.
- **Multi-tenant** : chaque entite porte un `tenant_id` extrait du claim JWT. Isolation testee.
- **HTTPS** : exige **par defaut** (desactive en profil `dev`).
- **Feign JWT Relay** : les appels inter-services forwardent automatiquement le token JWT.
- **Gateway** : point d'entree unique sur le port 8080, routage JWT vers les 4 backends.

## Flux principal

```
1. Creer un Site (location-service) → POST /api/sites
2. Creer un Checkpoint sur le Site → POST /api/sites/{siteId}/checkpoints
3. Creer un Colis (package-service) → POST /api/packages → recoit trackingNumber (ST-XXXXXXXX)
4. Enregistrer un scan au checkpoint → POST /api/checkpoints/scan → enregistre le passage
5. Le Package Service publie un evenement de changement de statut (outbox → RabbitMQ)
6. Le Tracking Service consomme l'evenement et enregistre la transition (Event Sourcing)
```

Tous les endpoints sont accessibles via le **Gateway** (`http://localhost:8080/api/...`) avec un JWT valide.

## Routes du Gateway

| Route | Backend | Port |
|---|---|---|
| `/api/packages/**` | package-service | 8081 |
| `/api/locations/**` | location-service | 8082 |
| `/api/sites/**` | location-service | 8082 |
| `/api/checkpoints/**` | location-service | 8082 |
| `/api/checkpoints/scan/**` | security-checkpoint-service | 8083 |
| `/api/checkpoint-logs/**` | security-checkpoint-service | 8083 |
| `/api/tracking/**` | tracking-service | 8084 |
| `/service/package/swagger-ui/**` | package-service (Swagger) | 8081 |
| `/service/location/swagger-ui/**` | location-service (Swagger) | 8082 |
| `/service/checkpoint/swagger-ui/**` | security-checkpoint-service (Swagger) | 8083 |
| `/service/tracking/swagger-ui/**` | tracking-service (Swagger) | 8084 |

## Tests

```bash
mvn clean verify
```

> `mvn verify` execute les tests **et** le controle de couverture JaCoCo (seuil minimal 80 %) pour les 8 modules.

## Documentation API (Swagger UI)

> Swagger UI est protege par JWT en production. En dev, il est accessible sans authentification.

- **Gateway** : `http://localhost:8080/swagger-ui/index.html`
- Package Service : `http://localhost:8081/swagger-ui/index.html`
- Location Service : `http://localhost:8082/swagger-ui/index.html`
- Security Checkpoint Service : `http://localhost:8083/swagger-ui/index.html`
- Tracking Service : `http://localhost:8084/swagger-ui/index.html`
- **Specs OpenAPI** : `docs/combined-openapi.yaml` (vue combinee) + une spec dediee par service.
- Les erreurs suivent **RFC 7807** (`application/problem+json`).

## Structure du depot

```
├── common-dto/                      # DTOs partages (PackageDto, events)
├── common-security/                 # JWT Resource Server + TenantFilter partage
├── gateway-service/                 # API Gateway (Spring Cloud Gateway)
├── package-service/                 # CRUD colis + outbox events
├── location-service/                # Localisation (Mongo) + Sites/Checkpoints (SQL)
├── security-checkpoint-service/     # Logs de scan securises
├── tracking-service/                # CQRS + Event Sourcing (Axon)
├── docs/                            # OpenAPI specs + script de generation
├── scripts/api-tests/               # Scripts de tests API (bash)
├── common/                          # Docker Compose + .env.example
└── docker-compose.yml               # Infrastructure complete
```

## Documentation produit

Vision, correctifs backend et architecture frontend (web + mobile) :

- [`docs/produit/README.md`](docs/produit/README.md) — sur la branche `docs-produit`
