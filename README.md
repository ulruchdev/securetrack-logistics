# CBS Onboarding - Projet 1 : Logistique

Système de microservices pour la gestion de la chaîne logistique CBS : suivi des colis, localisation des points de transition et enregistrement sécurisé des passages aux checkpoints.

## Architecture

| Service | Port | Base de données | Rôle |
|---|---|---|---|
| **package-service** | 8081 | PostgreSQL (`cbsdb`) | CRUD des colis (création, consultation, mise à jour partielle, suppression) |
| **location-service** | 8082 | MongoDB (`cbsdb`) | Localisation des colis — valide l'existence du colis via Package Service |
| **security-checkpoint-service** | 8083 | PostgreSQL (`cbsdb`) | Logs de passages sécurisés — valide la localisation via Location Service |
| **tracking-service** | 8084 | PostgreSQL (`cbsdb`) | Historique des transitions de statut — **CQRS + Event Sourcing** (Axon Framework) |

Communication inter-services : **OpenFeign** (Package Service ← Location Service ← Security Checkpoint Service). Le Tracking Service utilise **Axon Framework** (CQRS/Event Sourcing) pour enregistrer les transitions de statut. Documentation API OpenAPI 3.0.3 disponible dans le dossier [`docs/`](docs/).

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Docker + Docker Compose** (PostgreSQL, MongoDB, Redis)

## Démarrage rapide

### 1. Infrastructure (bases de données)

```bash
cd common
cp .env.example .env      # personnaliser les identifiants si besoin (fichier non versionné)
docker compose up -d
```

Les bases sont exposées **uniquement en local** (`127.0.0.1`) : PostgreSQL sur le port **5433**, MongoDB sur **27017**, Redis sur **6379**.

### 2. Variables d'environnement des services (OBLIGATOIRES)

> 🔑 Les mots de passe n'ont **plus de valeur par défaut** dans le code ni dans les `application.yml`.
> Un service **refuse de démarrer** si sa variable n'est pas définie.

```bash
# ⚠️ DB_PASSWORD et MONGO_URI doivent correspondre aux identifiants de l'infrastructure
#    définis dans common/.env (étape 1).
export DB_PASSWORD="<votre-mot-de-passe>"              # identique à POSTGRES_PASSWORD de common/.env
export MONGO_URI="mongodb://cbsuser:<votre-mot-de-passe>@localhost:27017/cbsdb?authSource=admin"  # = MONGO_USER:MONGO_PASSWORD
export JWT_SECRET="<clé-base64-≥32-bytes>"        # mot de passe Basic Auth interne (dev only)
```

| Variable | Obligatoire | Sert à | Correspondance (`common/.env`) |
|---|---|---|---|
| `DB_PASSWORD` | ✅ | Mot de passe PostgreSQL (package, security, tracking) | `POSTGRES_PASSWORD` |
| `MONGO_URI` | ✅ | URI de connexion MongoDB (location) | `MONGO_USER` : `MONGO_PASSWORD` |
| `SECURITY_PASSWORD` | ✅ | Mot de passe admin (interne, dev) | — (valeur libre) |
| `JWT_SECRET` | ✅ | Clé symétrique JWT (base64, ≥32 bytes) | — (valeur libre) |
| `DB_URL` / `DB_USERNAME` | ❌ (défauts locaux) | URL + utilisateur PostgreSQL | `POSTGRES_DB` / `POSTGRES_USER` |

### 3. Services

Depuis la racine du dépôt (après avoir exporté les variables ci-dessus) :

```bash
# Package Service (port 8081)
cd package-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Location Service (port 8082)
cd location-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Security Checkpoint Service (port 8083)
cd security-checkpoint-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Tracking Service (port 8084) — CQRS + Event Sourcing
cd tracking-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> 💡 **Schéma de base de données** : géré par **Liquibase** (migrations versionnées dans `db/changelog/`). `ddl-auto` est en `none` dans tous les profils — Hibernate ne modifie jamais le schéma ; chaque évolution passe par un nouveau changeset Liquibase.

## Configuration & sécurité

- **Secrets obligatoires, jamais versionnés** : les mots de passe (`DB_PASSWORD`, `MONGO_URI`, `JWT_SECRET`) n'ont **plus de valeur par défaut** dans le code ni dans les `application.yml` — ils sont lus depuis l'environnement au démarrage. Un service **refuse de démarrer** si sa variable est absente. La liste complète figure dans [`common/.env.example`](common/.env.example).
- **JWT Resource Server (OAuth2)** : les 4 services utilisent un validateur JWT partagé (`common-security`). En dev, une clé symétrique (`JWT_SECRET`) est utilisée. En prod, configurez `jwt.jwk-set-uri` vers votre IdP (Keycloak, Auth0, etc.).
- **Multi-tenant** : chaque entité porte un `tenant_id` extrait du claim JWT. Un tenant ne peut accéder qu'à ses propres données (isolation testée).
- **HTTPS** : exigé **par défaut** sur les endpoints protégés. Le profil `dev` le désactive.
- **En production** : définir des mots de passe forts et uniques — **aucune valeur n'est codée en dur dans le dépôt**.

> 🔧 **Dépannage** : si un service échoue au démarrage avec `password authentication failed` (PostgreSQL) ou une erreur de connexion MongoDB, c'est que la variable correspondante est absente ou ne correspond pas aux identifiants de l'infrastructure Docker — pas un bug de code.

## Tests

```bash
mvn clean verify
```

> `mvn verify` exécute les tests **et** le contrôle de couverture JaCoCo (seuil minimal 80 %) pour les 5 modules (`common-dto`, `package-service`, `location-service`, `security-checkpoint-service`, `tracking-service`).

## Produit cible (commercial)

Vision, correctifs backend et architecture frontend (web + mobile) :

- [`docs/produit/README.md`](docs/produit/README.md)

## Documentation API (dépôt actuel)

- **Swagger UI** :
  - Package Service : `http://localhost:8081/swagger-ui/index.html`
  - Location Service : `http://localhost:8082/swagger-ui/index.html`
  - Security Checkpoint Service : `http://localhost:8083/swagger-ui/index.html`
  - Tracking Service : `http://localhost:8084/swagger-ui/index.html`
- **Specs OpenAPI** : `docs/combined-openapi.yaml` (vue combinée) + une spec dédiée par service.
- Les erreurs suivent le standard **RFC 7807** (`application/problem+json`).

## Structure du dépôt

- `parent/` — POM parent commun (versions lombok, mapstruct, springdoc, compilation Java 21)
- `package-service/` — [README](package-service/README.md)
- `location-service/` — [README](location-service/README.md)
- `security-checkpoint-service/` — [README](security-checkpoint-service/README.md)
- `tracking-service/` — [README](tracking-service/README.md) — CQRS + Event Sourcing (Axon)
- `scripts/api-tests/` — scripts de tests des API (bash)
