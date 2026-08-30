# SecureTrack — Produit commercial (PRD)

**Statut** : décision product owner  
**Audience** : direction, commercial, produit, ingénierie  
**Périmètre** : produit **vendable** (SaaS B2B), pas le dépôt d’onboarding actuel  
**Documents liés** : [02-backend-remediation.md](02-backend-remediation.md) · [03-frontend-consommation.md](03-frontend-consommation.md)

---

## 1. Décisions product owner (figées)

Ces choix rendent les trois documents cohérents. Ils peuvent évoluer, mais **pas silencieusement** : toute revision doit mettre à jour les trois fichiers.

| Décision | Choix | Pourquoi |
|---|---|---|
| Marque produit | **SecureTrack** | Déjà le nom Maven (`securetrack-logistics`). CBS = **tenant / opérateur**, pas le nom public. |
| Modèle commercial | **SaaS B2B** (abonnement par organisation + sites) | Aligné marché TMS/WMS léger + visibilité colis, pas un outil interne unique. |
| Acheteur (confirmé) | **Mix ops + client** | L’ops paie ; le portail client justifie le renouvellement et le prix. |
| Langue produit V1 | **Français** (UI + docs). Codes métier et API en **anglais** (`IN_TRANSIT`, `trackingNumber`). | Marché francophone d’abord ; API stable pour intégrateurs. |
| Géographie V1 | **UE + Afrique francophone** (un déploiement cloud, données dans l’UE par défaut) | RGPD dès V1 ; latence Afrique via CDN + app mobile, pas un 2e cloud obligatoire. |
| Cœur métier V1 | Traçabilité **interne + client** d’un colis : création, sites/quais, checkpoints, historique, preuve de passage | Ni étiquettes transporteur, ni optimisation de tournées en V1 (marché déjà saturé). |
| Ce que nous ne sommes pas | Ni AfterShip (agrégateur multi-transporteurs), ni SAP TM, ni WMS complet | Positionnement : **tour de contrôle colis + checkpoint** pour 3PL, e-commerçants structurés, sites sécurisés. |

### Questions encore ouvertes (n’empêchent pas V1)

1. **Facturation** : par site, par colis/mois, ou forfait opérateurs ? (hypothèse V1 : forfait **organisation + N sites**, colis illimités jusqu’à un plafond soft).  
2. **Marque blanche** du portail client : hors V1 (URL `track.securetrack.app` + logo tenant).  
3. **Signature / photo POD** : V1.1, pas bloquant pour le premier contrat pilote.

---

## 2. Problème de marché

Les chargeurs et 3PL ont aujourd’hui :

- un **WMS ou Excel** pour l’entrepôt ;
- un **TMS ou le portail du transporteur** pour le last-mile ;
- **rien d’unifié** entre le quai, le checkpoint sécurité, et ce que voit le destinataire.

Conséquences constatées chez les acheteurs (ops + client) :

- Le destinataire appelle : « où est mon colis ? » alors que le colis est déjà passé 3 quais.  
- Le checkpoint sécurité note sur papier ou dans un outil déconnecté du statut colis.  
- Pas d’identifiant unique scannable de bout en bout.  
- Pas d’audit « qui a scanné quoi, où, quand » opposable en litige.

**SecureTrack** vend la **vérité unique du colis** : de la création à la livraison / perte, avec des **passages checkpoint** horodatés et un **lien de suivi** pour le client.

---

## 3. Proposition de valeur

**Pour** le directeur d’exploitation d’un 3PL ou d’un e-commerçant structuré,  
**SecureTrack** est la **plateforme de visibilité colis et de contrôle de passage**,  
**qui** donne aux opérateurs un scan fiable sur le quai et au destinataire un suivi clair,  
**contrairement** aux tableurs, aux portails transporteur fragmentés et aux microservices internes non industrialisés.

Promesse mesurable (contrat type) :

- Un colis a **un seul numéro de suivi public** (`ST-…`).  
- Tout passage checkpoint est **enregistré en moins de 2 s** en conditions réseau normales.  
- Le client voit le **même statut** que l’ops (délai de propagation inférieur à 5 s en V1 cloud).  
- Disponibilité cible **99,5 %** mensuelle hors maintenance planifiée (V1 ; 99,9 % en V2).

---

## 4. Personas et jobs

### 4.1 Acheteur

| Persona | Job principal | Critère de succès |
|---|---|---|
| **Directeur ops / 3PL** | Réduire litiges et appels « où est mon colis » | −30 % d’appels tracking en 90 jours |
| **RSSI / qualité** | Audit des passages | Export horodaté, utilisateur, résultat, site |

### 4.2 Utilisateurs ops

| Persona | Contexte | Job |
|---|---|---|
| **Agent quai** | Gants, bruit, Wi‑Fi instable | Scanner, confirmer, passer au colis suivant |
| **Agent checkpoint** | Contrôle sécurité | OK / refus / alerte + commentaire court |
| **Superviseur** | Bureau + tablette | File du jour, exceptions, colis bloqués |
| **Admin tenant** | Bureau | Sites, quais, utilisateurs, rôles |

### 4.3 Utilisateur client (non-payeur, critique pour le renouvellement)

| Persona | Job |
|---|---|
| **Destinataire / chargeur** | Voir statut + historique sans créer de compte (lien + n° de suivi) |

---

## 5. Périmètre produit

### 5.1 V1 — commercialisable (MVP payant)

Inclus :

1. **Organisation (tenant)** multi-sites.  
2. **Référentiel lieux** : Site → Zone / quai → Checkpoint (un quai sert **N colis**, pas 1 colis = 1 location).  
3. **Colis** : création, métadonnées (poids, fragile, type), **numéro de suivi public**, statut métier.  
4. **Cycle de vie** : `NEW` → `IN_TRANSIT` → `DELIVERED` \| `LOST` (extensions V1.1 : `HELD`, `RETURNED`).  
5. **Checkpoint** : scan du n° de suivi, résultat, agent authentifié, site connu.  
6. **Historique immuable** des transitions (source de vérité pour le portail).  
7. **Console web ops** : listes, fiche colis, exceptions, admin sites/users.  
8. **App mobile terrain** : scan + file locale hors-ligne + synchro.  
9. **Portail client web** : page publique suivi (n° + éventuellement token lien).  
10. **Notifications** V1 : e-mail sur `IN_TRANSIT` et `DELIVERED` (SMS = V1.1).  
11. **SSO / comptes** : un utilisateur = un agent ; rôles (voir §7).  
12. **API publique versionnée** pour intégration ERP/WMS (clé API par tenant).

Exclu V1 (volontaire) :

- Génération d’étiquettes transporteur, tarification, booking.  
- Optimisation de tournées, GPS chauffeur continu.  
- Facturation transporteur, douane formalisée, IoT.  
- WMS (emplacements picking, inventaire).  
- Marketplace / app store.

### 5.2 V1.1 — pour rester compétitif à 6 mois

- Preuve de livraison (photo + signature).  
- Statuts `HELD` / `RETURNED`.  
- SMS / WhatsApp Business.  
- Webhooks sortants signés (HMAC).  
- Mode sombre optionnel mobile (quai de nuit).

### 5.3 V2 — expansion marché

- GPS / dernière position connue (opt-in).  
- Agrégation n° transporteur externe (type AfterShip, partenaire).  
- Multi-région données, marque blanche.  
- SLA 99,9 %, SOC2 / ISO 27001.

---

## 6. Parcours cibles (V1)

### 6.1 Création → suivi client

1. L’ops crée le colis (console ou API).  
2. Le système attribue `trackingNumber` + code-barres / QR.  
3. Un e-mail (optionnel) part au destinataire avec le lien portail.  
4. Le destinataire ouvre le lien : statut + timeline.

### 6.2 Passage quai / checkpoint (cœur terrain)

1. L’agent ouvre l’app, site pré-sélectionné (ou scan du quai).  
2. Scan du colis.  
3. Si hors-ligne : événement en file locale, UI « en attente de synchro ».  
4. Si en ligne : ACK serveur, statut mis à jour, timeline enrichie.  
5. En cas de `REFUSED` / `ALERT` : le superviseur voit une exception.

### 6.3 Litige

1. L’ops ouvre la fiche colis.  
2. Timeline : transitions + checkpoints (qui, où, résultat).  
3. Export PDF / CSV pour le chargeur.

---

## 7. Modèle métier (langue commune backend / frontend)

### 7.1 Identifiants (non négociable)

| Concept | ID interne | ID public / scan |
|---|---|---|
| Organisation | UUID | slug (`acme`) |
| Site, zone, checkpoint | UUID | code court affichable (`CDG-Q3`) |
| Colis | UUID | **`trackingNumber`** `ST-` + alphabet sans ambiguïté (pas de `0/O`) |
| Utilisateur | UUID (IdP) | e-mail |
| Événement checkpoint | UUID | — |
| Événement tracking | UUID (event store) | — |

**Interdit en produit** : exposer un `Long` auto-incrémenté comme identifiant client ou scan. Les IDs numériques actuels restent éventuellement **internes** le temps d’une migration.

### 7.2 Lieux

Un **Site** a des **Checkpoints**. Un colis **n’est pas** une location.  
La « position actuelle » du colis = **dernier checkpoint réussi** (ou dernier événement de tracking avec `siteId` / `checkpointId`).

### 7.3 Statuts et résultats

- Statut colis : machine d’état unique (package-service = source d’écriture statut).  
- Résultat checkpoint : `OK` | `REFUSED` | `ALERT` | `PENDING` — n’écrase le statut colis **que** selon des règles explicites (ex. `ALERT` → exception, pas forcément `LOST`).

### 7.4 Rôles V1

| Rôle | Console web | Mobile | Portail client | API |
|---|---|---|---|---|
| `TENANT_ADMIN` | tout le tenant | — | — | clés API |
| `SUPERVISOR` | ops + exceptions | lecture / déblocage | — | — |
| `CHECKPOINT_OPERATOR` | lecture limitée | scan + résultat | — | — |
| `SHIPPER_USER` | colis de son compte | — | — | optionnel |
| Anonyme + token lien | — | — | suivi d’**un** colis | — |

---

## 8. Exigences marché (non fonctionnelles)

| Domaine | Exigence V1 |
|---|---|
| Disponibilité | 99,5 % mensuel |
| Latence API p95 | < 300 ms lecture ; < 800 ms scan (hors cold start) |
| Hors-ligne mobile | File locale, retry exponentiel, idempotence serveur |
| Sécurité | OIDC (Keycloak ou équivalent), TLS partout, pas de Basic Auth produit, pas de Swagger public prod |
| Données | Hébergement UE, DPA, droit d’accès / suppression (RGPD) |
| Audit | Immuabilité des événements tracking ; logs checkpoint non effaçables par l’opérateur |
| Support | Statuts HTTP et `application/problem+json` stables ; corrélation `X-Request-Id` |

---

## 9. Offre commerciale (esquisse)

| Offre | Contenu | Pour qui |
|---|---|---|
| **Starter** | 1 site, 10 opérateurs, portail client, e-mail | pilote |
| **Business** | N sites, SSO, API, exports, SLA 99,5 % | 3PL / e-com |
| **Enterprise** | SSO entreprise, webhooks, DPA renforcé, horaires support | groupes |

Le premier contrat **pilote payant** (même symbolique) est le critère « commercialisable », pas la couverture AfterShip.

---

## 10. Critères d’acceptation « on peut vendre »

Le produit est **commercialisable** lorsque **tous** les points suivants sont vrais :

1. Un tenant isolé (données non mélangées).  
2. Un agent scanne un colis sur mobile **sans ordinateur**.  
3. Le destinataire suit le colis **sans compte**.  
4. Un admin crée sites, checkpoints, utilisateurs.  
5. Auth réelle (pas un seul `admin` en mémoire).  
6. Déploiement reproductible (images + env), HTTPS, backups testés.  
7. Aucun 500 pour une erreur métier ; PATCH vraiment partiel.  
8. Contrat d’API v1 figé + changelog.  
9. Parcours litige exportable.  
10. Conditions d’utilisation + politique de confidentialité.

Le dépôt actuel **ne satisfait aucun** de ces 10 points de bout en bout. Le document 2 dit comment le backend y arrive ; le 3, comment le frontend les consomme.

---

## 11. KPIs produit

| KPI | Cible 90 jours post-pilote |
|---|---|
| Scans réussis / scans tentés | ≥ 98 % |
| Délai création → premier scan | mesuré (pas de cible unique) |
| Appels support « où est mon colis » | −30 % vs baseline client |
| Temps scan médian (UI) | < 8 s porte-à-porte |
| Événements en attente hors-ligne > 24 h | < 0,5 % |

---

## 12. Glossaire

| Terme | Sens SecureTrack |
|---|---|
| **Tenant** | Organisation cliente (CBS, Acme, …) |
| **Site** | Entrepôt / agence physique |
| **Checkpoint** | Point de contrôle scannable sur un site |
| **Tracking number** | Identifiant public du colis |
| **Timeline** | Suite ordonnée d’événements (statut + passages) |
| **Exception** | Checkpoint `REFUSED`/`ALERT` ou colis bloqué |
| **BFF** | Backend-for-frontend : unique entrée HTTP des apps |

---

## 13. Traçabilité documents

| Besoin produit | Backend (doc 2) | Frontend (doc 3) |
|---|---|---|
| Scan terrain | Idempotence, offline contract, auth agent | App mobile |
| Portail client | API publique lecture, token lien | Web public |
| Vérité unique | Outbox, IDs UUID, sites master data | Timeline unifiée |
| Vendre | Gateway, observabilité, isolation tenant | Console + onboarding tenant |
