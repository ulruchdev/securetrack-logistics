# SecureTrack — Backend : failles, manques, correctifs

**Statut** : backlog d’ingénierie imposé par le produit ([01-produit-commercial.md](01-produit-commercial.md))  
**Périmètre** : les 4 services actuels + ce qu’il **faut ajouter** pour un backend vendable  
**Cible API** : celle que le frontend doit consommer ([03-frontend-consommation.md](03-frontend-consommation.md))

Ce document **corrige et étend** le backend d’onboarding. Il n’est pas une liste de souhaits : chaque item est soit un **défaut actuel**, soit un **manque produit**.

---

## 1. Principes (non négociables)

1. **Un contrat HTTP unique** pour les clients : API Gateway / BFF. Les apps ne parlent plus aux ports 8081–8084.  
2. **Identifiants produit** : UUID + `trackingNumber` (doc 1 §7.1). Les `Long` actuels sont une dette de migration.  
3. **Erreurs métier ≠ 500**. RFC 7807 partout, codes stables (`type` URI).  
4. **Pas de double écriture fragile** : statut colis + événement tracking via **outbox** (ou transaction unique), jamais `catch` + log warn.  
5. **Authn/Authz** sur **tous** les endpoints ops. Lecture publique **bornée** (un colis, token).  
6. **Idempotence** sur tout POST de scan / checkpoint (`Idempotency-Key`).  
7. Schéma **versionné** (Liquibase). `ddl-auto: update` **interdit** en production, y compris Axon (script SQL figé).

---

## 2. Cartographie de l’existant (constat)

| Composant | Rôle actuel | Verdict produit |
|---|---|---|
| `package-service` | CRUD colis, machine d’état, publication Rabbit | Cœur utile ; **sans auth**, IDs `Long`, `locationId` jamais publié dans l’événement |
| `location-service` | Document Mongo **1 colis ↔ 1 location** (index unique `packageId`) | **Modèle faux** pour un entrepôt ; à remplacer par un référentiel sites |
| `security-checkpoint-service` | Logs de passage, Feign + Redis + CB, Basic Auth 1 user | Cœur utile ; auth inadaptée ; dépend du mauvais modèle location |
| `tracking-service` | CQRS/ES Axon, listener Rabbit | Cœur utile ; IDs `String` incohérents ; `ddl-auto: update` ; pas d’auth alignée IdP |
| `common-dto` | `PackageDto`, événement statut | À versionner (compatibilité Jackson) |
| Compose `common/` | Postgres, Mongo, Redis, Rabbit **localhost** | Dev only |
| Compose racine | Postgres « SecureTrack » orphelin | À fusionner / supprimer |
| CI | `mvn verify` + GitGuardian ; **pas d’images** des services | Insuffisant |
| Dockerfiles services | **Absents** | Bloquant prod |

---

## 3. Failles et bugs à corriger (code actuel)

Priorité : **P0** bloquant commercial / données ; **P1** qualité contrat API ; **P2** durcissement.

### 3.1 Intégrité et événements — P0

| ID | Faille | Correctif |
|---|---|---|
| B-01 | Publication Rabbit dans `PackageService.publishStatusChanged` : l’échec est **avalé** (`log.warn`). Le colis est persisté, le tracking **perd** l’événement. | Pattern **outbox** (table `outbox` même TX que le colis) + publisher. Si outbox échoue, **rollback** du PATCH. |
| B-02 | `locationId` de `PackageStatusChangedEvent` est **toujours `null`** côté package-service. | Le scan checkpoint (ou un `lastCheckpointId`) alimente le tracking. Le PATCH statut ops peut envoyer `checkpointId` / `siteId` optionnels. |
| B-03 | RabbitMQ password défaut `guest` dans `application.yml` package/tracking. | Même règle que `DB_PASSWORD` : **obligatoire**, pas de défaut. |
| B-04 | Listener tracking : NACK + rethrow = **retry infini** si l’événement est poison (ex. statut impossible). | DLQ + max retries + alerte. Idempotence aggregate (même `eventId`). |
| B-05 | Deux chemins d’écriture tracking : POST manuel `/api/tracking` **et** Rabbit. Risque de **doublons / divergences**. | V1 produit : **une seule** source d’écriture métier (package + checkpoint). POST tracking réservé admin/replay, pas au frontend ops. |

### 3.2 Modèle location — P0 produit

| ID | Faille | Correctif |
|---|---|---|
| B-06 | `Location.packageId` unique : un lieu = un colis. Inutilisable sur un quai. | Nouveau modèle : `Site` / `Zone` / `Checkpoint` **sans** packageId. Position colis = dernier événement. |
| B-07 | Création location exige un `packageId` existant (Feign). Couplage inverse du métier. | Le référentiel lieux est autonome. Le colis **référence** `currentSiteId` / `currentCheckpointId` (nullable). |
| B-08 | Mongo pour des entités référentielles petites et relationnelles. | V1 : **PostgreSQL** pour sites/checkpoints (même `cbsdb` ou schéma `location`). Mongo seulement si un vrai besoin documentaire apparaît (hors V1). |

### 3.3 Contrats HTTP et erreurs — P1

| ID | Faille | Correctif |
|---|---|---|
| B-09 | Scripts API historiques : 500 sur cas métier. Package mappe déjà `IllegalArgumentException` → 400 ; **vérifier** location/checkpoint (exceptions Feign vs métier). Harmoniser : 404 colis inconnu, 409 transition, 422 checkpoint indisponible. | Catalogue `type` : `https://api.securetrack.app/problems/invalid-transition`, etc. Tests contrat. |
| B-10 | Messages d’erreur **anglais** dans `validateStatusTransition`. | Messages i18n ou français + `code` machine (`INVALID_TRANSITION`). Le frontend affiche via `code`. |
| B-11 | `PATCH` : MapStruct `IGNORE` null est **déjà** en place. Conserver ; **tests** d’intégration pour empêcher la régression (doc scripts encore pessimiste). | Tests Testcontainers PATCH partiel. |
| B-12 | Pagination : package borne `size` ; location/checkpoint **non**. | Borne 1–100 partout (CWE-400). |
| B-13 | OpenAPI combinée : 4 `servers` localhost, pas de gateway, Basic Auth partiel. | Une spec **Gateway** `openapi/v1.yaml` ; génération clients front. |

### 3.4 Sécurité — P0

| ID | Faille | Correctif |
|---|---|---|
| B-14 | Package + location : **aucune** auth. | Resource server JWT (OIDC). |
| B-15 | Checkpoint + tracking : **Basic Auth**, un user **in-memory**. | JWT + rôles doc 1 §7.4. Plus de mot de passe partagé d’équipe. |
| B-16 | Swagger / `v3/api-docs` **permitAll**. | Désactivé en prod ; protégé en staging. |
| B-17 | CSRF désactivé + Basic : acceptable API, **insuffisant** cookies session. | API **Bearer only** (SPA/mobile). |
| B-18 | HTTPS forcé checkpoint hors `dev` ; tracking **sans** équivalent. | TLS au **gateway** ; services internes mTLS ou réseau privé. |
| B-19 | Redis **sans** AUTH dans Compose. | `requirepass` / ACL. |
| B-20 | Isolation **tenant** absente. | `tenant_id` sur toutes les tables ; filtre Hibernate / query obligatoire. Jamais d’ID global devinable entre tenants. |

### 3.5 Résilience et ops — P0/P1

| ID | Faille | Correctif |
|---|---|---|
| B-21 | Chaîne Feign synchrone checkpoint → location → package. Panne en cascade. | Checkpoint V1 : valider colis par **package-service** (JWT service-to-service) ; valider `checkpointId` en base locale. Moins de sauts. CB conservé. |
| B-22 | Pas d’**idempotence** scan. Double tap mobile = double log. | Clé `Idempotency-Key` (UUID client) unique par tenant, TTL 24–72 h. |
| B-23 | Pas de **Dockerfile** / health pour orchestre. | Image par service ; `GET /actuator/health` (liveness) + readiness (DB, Rabbit). |
| B-24 | Actuator : health seulement ; CB exposés. | Prometheus metrics ; **ne pas** exposer `circuitbreakerevents` publiquement. |
| B-25 | Axon `ddl-auto: update`. | Schéma Axon **figé** (SQL officiel / documenté) + `validate` en prod. |
| B-26 | CI : `echo "Docker build ready"` ; Compose racine ≠ `common/`. | Build images, scan (Trivy), tests Testcontainers, un seul Compose. |
| B-27 | Logs Mongo `DEBUG` en config location. | `INFO` par défaut ; DEBUG via env. |
| B-28 | Suppression colis `DELETE` : casse l’audit / le portail. | **Pas de delete physique** V1 : `CANCELLED` ou archivage. RGPD = procédure admin, pas un DELETE agent. |

### 3.6 Tests — P1

| ID | Faille | Correctif |
|---|---|---|
| B-29 | Quasi uniquement `@WebMvcTest` + mocks. | Testcontainers : Postgres + Rabbit + Redis ; scénario colis → checkpoint → timeline. |
| B-30 | Scripts bash : 3 services, pas tracking ; anomalies peut-être **obsolètes**. | Remplacer par tests Spring + contrat OpenAPI (Prism / Schemathesis) en CI. |

---

## 4. Architecture backend cible (V1)

```text
                    [Console web] [Portail public] [App mobile]
                                    |
                              TLS + JWT / token lien
                                    |
                            API Gateway / BFF
                         /api/v1/...  (seul contrat front)
                                    |
          +-------------+-----------+-----------+--------------+
          |             |           |           |              |
     identity       package      sites     checkpoint      tracking
     (OIDC)         (écriture     (master    (scans)        (lecture
                     statut)      data)                      timeline)
          |             |                         |
          |             +-------- outbox ---------+
          |             |                         |
          |             v                         v
          |          RabbitMQ / Kafka          event store
          |          (événements internes)
          |
     PostgreSQL (tenant, colis, sites, checkpoints, outbox, projections)
     Redis (cache, idempotency keys, rate-limit)
```

**Conservation Axon** : oui pour l’historique immuable, **derrière** le tracking-service. Le frontend **ne parle pas** à Axon.

**Service-to-service** : JWT (`client_credentials`) ou mTLS, plus d’appels Feign anonymes.

---

## 5. Contrat API v1 (ce que le backend expose au BFF)

Base : `https://api.securetrack.app/v1`  
Auth : `Authorization: Bearer <access_token>` sauf routes `public`.

### 5.1 Ressources (minimum V1)

| Méthode | Chemin | Rôle | Idempotent |
|---|---|---|---|
| GET | `/me` | Profil + tenant + sites autorisés | oui |
| GET/POST | `/sites`, `/sites/{id}/checkpoints` | Référentiel | POST avec clé |
| POST | `/packages` | Création colis | clé |
| GET | `/packages?status=&siteId=&q=` | Liste ops | oui |
| GET | `/packages/{id}` | Fiche (UUID interne) | oui |
| PATCH | `/packages/{id}` | Métadonnées ; statut **ops seulement** si pas de scan | oui (If-Match version) |
| POST | `/checkpoints/scans` | **Scan terrain** (trackingNumber + checkpointId + result) | **Idempotency-Key** obligatoire |
| GET | `/packages/{id}/timeline` | Fusion transitions + scans | oui |
| GET | `/exceptions` | File superviseur | oui |
| GET | `/public/track/{trackingNumber}` | Portail ; éventuellement `?token=` | oui + rate-limit strict |

**Interdit au frontend V1** : appeler `POST /api/tracking` des 4 services actuels ; créer une « location » liée à un packageId.

### 5.2 Corps scan (exemple)

```json
{
  "trackingNumber": "ST-7K4M2P",
  "checkpointId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "result": "OK",
  "comment": "Contrôle visuel"
}
```

Réponse 201 : événement + colis (etag `version`) + statut éventuellement mis à jour (`IN_TRANSIT` au premier scan OK).

### 5.3 Erreurs

Toujours `application/problem+json` :

- `title`, `status`, `detail`, `instance`, `code` (machine), `traceId`  
- 401 / 403 / 404 / 409 (If-Match) / 422 métier / 429 / 503  

---

## 6. Évolutions par service

### 6.1 Nouveau : `identity` / IdP

- Keycloak (ou Auth0 en SaaS) : realms par **environnement**, clients `web`, `mobile`, `bff`.  
- Claims : `tenant_id`, `roles`, `site_ids`.  
- Pas de users Spring `InMemory`.

### 6.2 Nouveau : `gateway` / `bff`

- Spring Cloud Gateway **ou** BFF Node/Nest **uniquement** si l’équipe front l’opère ; défaut recommandé : **Gateway Java** pour rester dans le repo actuel.  
- CORS strict, rate-limit, corrélation, agrégation `timeline` (évite 3 round-trips mobile).

### 6.3 `package-service`

- Colonnes : `id UUID`, `tenant_id`, `tracking_number` unique par tenant, `version`, `current_checkpoint_id`, pas de DELETE physique.  
- Auth + tenant filter.  
- Outbox.  
- `If-Match` / `@Version` déjà présent : l’exposer en header.

### 6.4 `location-service` → `site-service` (rename logique)

- Entités Site, Checkpoint (geo optionnelle : lat/lng V1.1).  
- CRUD admin.  
- Plus de Feign vers package à la création d’un lieu.

### 6.5 `security-checkpoint-service`

- Scan par `trackingNumber` + `checkpointId`.  
- `createdBy` = **sub JWT**, plus un champ libre `agent001`.  
- Redis : cache checkpoint + store idempotency.

### 6.6 `tracking-service`

- Aggregate id = `package UUID`.  
- Projection `timeline` lue par le BFF.  
- Listener **idempotent** (`eventId` outbox).  
- Auth lecture ops ; pas d’écriture front.

---

## 7. Données, migration, dépréciation

1. Introduire UUID **en parallèle** des `Long` (`legacy_id`) si des données de démo existent.  
2. Générer `trackingNumber` pour chaque colis existant.  
3. Migrer documents Mongo location → tables SQL **sites** (données démo recréées si besoin).  
4. OpenAPI actuelle : marquer `deprecated` ; sunset après V1 gateway.

---

## 8. Observabilité, perf, charge

| Sujet | V1 |
|---|---|
| Logs | JSON structuré, `tenantId`, `traceId`, pas de PII dans les URL logs |
| Metrics | Micrometer + Prometheus : latence scan, file outbox, CB, DLQ |
| Tracing | OpenTelemetry (gateway → services) |
| Charge indicative | 50 scans/s par tenant gros : connection pool Hikari dimensionné ; pas de N+1 timeline |
| Backup | Postgres PITR ; Redis non source de vérité |

---

## 9. Ordre d’implémentation (backend seul)

| Sprint thème | Livrable | Débloque |
|---|---|---|
| **S0** | Dockerfiles, Compose unique, secrets, health, plus de `guest` | Ops interne |
| **S1** | JWT partout, Swagger fermé, tenant_id | Sécurité |
| **S2** | Site/Checkpoint SQL, abandon 1-colis-1-lieu | Modèle métier |
| **S3** | Outbox + DLQ + eventId ; locationId/checkpoint sur événements | Vérité unique |
| **S4** | `POST /scans` idempotent ; plus de DELETE dur | Terrain |
| **S5** | Gateway `/v1` + timeline agrégée + public track + rate-limit | Front (doc 3) |
| **S6** | Testcontainers E2E, OpenAPI v1, Axon ddl figé, metrics | Commercialisation tech |

Les sprints front (doc 3) **démarrent en parallèle de S5**, pas avant : sinon le front se couple aux 4 APIs mortes.

---

## 10. Définition of Done backend (critères doc 1 §10)

Un item backend est **Done** seulement si :

- test automatisé du cas nominal **et** d’erreur (409/422, pas 500) ;  
- OpenAPI gateway à jour ;  
- pas de secret par défaut ;  
- tenant isolé vérifié par un test « cross-tenant 404 ».

Sans S0–S6, le produit **n’est pas** commercialisable côté serveur, même avec une UI magnifique.
