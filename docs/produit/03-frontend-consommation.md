# SecureTrack — Frontend : consommation du backend (web + mobile)

**Statut** : architecture et exigences UX/qualité pour les clients du backend cible  
**Dépend de** : [01-produit-commercial.md](01-produit-commercial.md) (parcours) · [02-backend-remediation.md](02-backend-remediation.md) (API `/v1`)  
**Règle** : le frontend consomme **uniquement** le Gateway `/v1`. Jamais les ports 8081–8084, jamais Axon, jamais Rabbit.

---

## 1. Intention

Trois surfaces, **une** vérité colis :

| Surface | Utilisateur | Job | Priorité V1 |
|---|---|---|---|
| **Console ops** (web) | Admin, superviseur, parfois agent bureau | Listes, fiche, sites, users, exceptions | P0 |
| **Portail client** (web public) | Destinataire / chargeur | Suivre un colis sans compte | P0 |
| **App terrain** (mobile) | Agent quai / checkpoint | Scanner en moins de 8 s, y compris hors-ligne | P0 |

Un **site marketing** (pricing, preuve sociale) est **hors** de l’app produit. Il peut réutiliser la charte (§6) mais pas le design dense de la console.

---

## 2. Raisonnement d’architecture

### 2.1 Pourquoi un Gateway / BFF

Aujourd’hui il faudrait 3–4 bases URL, 2 schémas d’auth, et assembler timeline à la main. Sur mobile (latence, batterie, hors-ligne), c’est inacceptable.

Le BFF :

- un seul host, un seul schéma d’erreur ;  
- agrège `GET /packages/{id}/timeline` ;  
- attache le JWT ;  
- rate-limit le portail public.

Le front **n’implémente pas** la machine d’état colis.

### 2.2 Pourquoi pas de monolithe web « qui appelle Feign »

La console n’est pas un reverse-proxy métier. Toute règle (transition, tenant) reste **serveur**.

### 2.3 Pourquoi web **et** mobile natif (pas un seul PWA pour le quai)

Un PWA peut suffire au **bureau**. Sur un quai :

- scan caméra fiable + retour haptique ;  
- file hors-ligne **durable** (SQLite), pas seulement le cache HTTP ;  
- usage gants → cibles 44×44 px, un pouce.

V1 : **app native** (Expo) pour le scan ; la console reste **web**. Pas de scan critique dans le navigateur entrepôt en V1 (option V1.1 si un client n’a que des tablettes Chrome).

---

## 3. Stack technique retenue

| Couche | Choix | Pourquoi |
|---|---|---|
| Console ops + portail | **Next.js** (App Router) + TypeScript | SSR portail (SEO lien suivi), auth cookie BFF possible, un langage avec le mobile |
| Mobile | **Expo (React Native)** + TypeScript | Même types, OpenAPI → client TS unique, itération OTA prudente |
| UI console | **Tailwind + composants type shadcn/ui** | Dense, accessible, pas de kit « landing page » |
| État serveur | **TanStack Query** | Cache, retry, invalidation timeline après scan |
| Hors-ligne mobile | **SQLite** (expo-sqlite) + file d’outbox locale | Aligné idempotency serveur |
| Scan | `expo-camera` / lib code-barres | V1 : code-barres + QR du `trackingNumber` |
| Auth | OIDC **Authorization Code + PKCE** (mobile) ; console : même IdP, **BFF** ou SPA public client | Pas de mot de passe dans l’app ; pas de Basic Auth |
| Client HTTP | Client **généré** depuis OpenAPI `v1` | Une source de vérité avec le doc 2 |
| Monorepo | `apps/web`, `apps/mobile`, `packages/api-client`, `packages/domain` | Types `PackageStatus`, `ScanResult` partagés |

**Écartés** :

- Flutter : excellent terrain, mais **deux** langages vs backend TS client.  
- App web unique responsive pour le scan P0.  
- Appeler Spring Security Basic depuis le navigateur (CORS + mot de passe partagé).

---

## 4. Comment consommer le backend

### 4.1 Environnements

| Env | Base URL | Auth |
|---|---|---|
| Dev | `http://localhost:8080/v1` (gateway) | realm `dev` |
| Staging / prod | `https://api.securetrack.app/v1` | realm dédié |

Interdit : hardcoder `localhost:8081`.

### 4.2 Mapping écrans → API

**Console**

| Écran | Appels |
|---|---|
| Login | Redirect IdP → tokens |
| Liste colis | `GET /packages` (filtres, page) |
| Fiche colis | `GET /packages/{id}` + `GET /packages/{id}/timeline` |
| Création | `POST /packages` + Idempotency-Key |
| Sites / checkpoints | `GET/POST /sites...` |
| Exceptions | `GET /exceptions` |
| Admin users | **IdP** (Keycloak admin) ou API users si exposée ; pas un CRUD maison divergent |

**Mobile**

| Écran | Appels |
|---|---|
| Site actif | `GET /me` (sites autorisés), stockage local |
| Scan | `POST /checkpoints/scans` + `Idempotency-Key` (UUID généré **avant** l’envoi, persisté dans l’outbox locale) |
| Échec réseau | Rester dans l’outbox ; replay à la reconnexion **dans l’ordre** |
| Détail après scan | Utiliser la réponse 201 ; sinon `GET` fiche par tracking number si le gateway l’expose |

**Portail public**

| Écran | Appels |
|---|---|
| `/t/{trackingNumber}` | `GET /public/track/{trackingNumber}` |
| Lien e-mail | même route + `token` query si le backend le exige |

Pas de JWT destinataire en V1.

### 4.3 Contrats que le front **doit** respecter

1. **Idempotency-Key** : même clé si retry (timeout). Jamais une nouvelle clé sur le même scan physique.  
2. **If-Match** : si la console PATCH un colis, envoyer `version`. Conflit 409 → refetch, pas d’overwrite silencieux.  
3. **Erreurs** : lire `code` + `status` ; ne jamais parser le texte `detail` pour la logique.  
4. **401** : refresh token ; échec → login. **403** : message « pas le droit sur ce site ».  
5. **429** portail : message calme, pas de retry agressif (boucle).  
6. **Pas de DELETE** colis dans l’UI agent.

### 4.4 Ce que le front ne consomme pas

- `/api/locations` liées à `packageId`  
- `POST /api/tracking`  
- Swagger UI en production  
- Actuator  

---

## 5. Structure des applications

### 5.1 Console (information dense)

Layout : barre latérale (Colis, Exceptions, Sites, Réglages) + zone principale.

Parcours critiques :

1. Recherche `trackingNumber` (raccourci clavier).  
2. Fiche : statut, site actuel, timeline verticale (scan + changement statut).  
3. Exception : CTA « voir colis », pas de dashboard vanity metrics en V1.

États UI obligatoires : loading (skeleton), vide, erreur + retry, 409 conflit.

### 5.2 Portail client (lisibilité)

Une colonne, timeline, **pas** de jargon interne (`UUID`, `IN_TRANSIT` brut). Libellés FR : « En transit », « Livré », « Perdu ».

Pas de compte. Pas de chat. Lien « contacter l’expéditeur » = mailto configuré tenant (V1.1).

### 5.3 Mobile terrain (une main)

Flux unique :

1. Écran scan (plein écran caméra + saisie manuelle du n°).  
2. Confirmation : tracking, site, 4 gros boutons résultat (`OK` primaire, `REFUSED` / `ALERT` destructifs).  
3. ACK vert / file « 3 en attente de synchro ».

Interdit : listes type console, création de colis, admin users.

Hors-ligne : l’agent **peut** scanner ; l’UI montre clairement « non synchronisé ». Interdit de faire croire que le serveur a ACK.

---

## 6. Design system produit (qualité visuelle)

Charte **ops** (console + mobile) — professionnelle, contrastée, pas une landing « social proof » :

| Token | Valeur | Usage |
|---|---|---|
| Primary | `#2563EB` | Actions, liens, scan OK |
| CTA / accent | `#F97316` | Action principale mobile (déclencher scan) |
| Texte | `#0F172A` sur fond `#F8FAFC` | Contraste ≥ 4,5:1 |
| Danger | rouge sémantique WCAG | `REFUSED` / `ALERT` |
| Police | **Plus Jakarta Sans** | Console et portail ; mobile : même famille ou system si perf |

Règles :

- Icônes SVG (Lucide), **jamais** d’emoji comme icône.  
- Focus visible, navigation clavier console.  
- Cibles tactiles ≥ 44×44 px.  
- Transitions 150–300 ms ; `prefers-reduced-motion`.  
- Light mode **par défaut** (quai de jour). Sombre = V1.1 mobile.  
- Breakpoints web : 375 / 768 / 1024 / 1440. Console **min 1280** confort ; en dessous, message « utilisez une tablette paysage ou l’app ».

Le site **marketing** (hors V1 app) peut insister sur logos clients et métriques ; **pas** la console.

---

## 7. Qualité, accessibilité, perf

| Sujet | Exigence |
|---|---|
| a11y | WCAG 2.2 AA sur portail et console (contraste, labels, focus) |
| i18n | UI FR V1 ; clés i18n dès le départ (EN V1.1) |
| Perf web | LCP portail < 2,5 s 4G ; pas de JS inutile sur `/t/...` |
| Perf mobile | Démarrage scan < 2 s à chaud ; caméra permission expliquée |
| Observabilité front | `traceId` affiché en écran erreur ops ; Sentry (PII off) |
| Tests | Playwright : login → liste → fiche. Mobile : Detox ou Maestro — scan mocké + outbox |
| Sécurité front | Pas de token dans `localStorage` si BFF cookie httpOnly possible sur web ; mobile : secure storage |

---

## 8. Hors-ligne (contrat avec le backend)

```text
[Scan UI] → enregistre OutboxLocale { key, payload, createdAt }
         → tente POST /checkpoints/scans
         → 2xx : marque synced, invalide cache fiche
         → 409 duplicate key : traiter comme succès (déjà appliqué)
         → 422 métier : sortir de la file, afficher erreur (ne pas retrier en boucle)
         → réseau : rester queued
```

Le backend (doc 2 B-22) **garantit** que la même `Idempotency-Key` ne crée pas deux passages. Le front **garantit** de réutiliser la clé.

---

## 9. Ordre d’implémentation frontend

| Phase | Livrable | Prérequis backend |
|---|---|---|
| **F0** | Monorepo, api-client généré (spec mock si besoin) | Spec OpenAPI v1 même **partielle** |
| **F1** | Portail public (timeline lecture) | `GET /public/track/{n}` |
| **F2** | Console : login, liste, fiche | JWT + `GET /packages*` + timeline |
| **F3** | Mobile scan + outbox | `POST /scans` + idempotence |
| **F4** | Console exceptions + admin sites | `GET /exceptions`, CRUD sites |
| **F5** | Polish a11y, empty/error, Sentry | Gateway stable |

**Ne pas** construire F3 contre l’API checkpoint Basic Auth actuelle : ce serait à jeter.

---

## 10. Définition of Done frontend

Un écran est Done si :

- il n’utilise que `/v1` ;  
- les 4 états (OK, vide, chargement, erreur) existent ;  
- un cas 401/403/409/422 est géré explicitement ;  
- (mobile scan) l’outbox survit à un kill de l’app ;  
- a11y de base (label, focus, contraste) vérifiée.

Aligné doc 1 §10 : sans **F1 + F2 + F3**, le produit n’est pas commercialisable, même si S0–S6 backend sont verts.

---

## 11. Synthèse de dépendance

```text
Doc 1  parcours + personas
  ↓
Doc 2  /v1 + scans idempotents + tenant + JWT     ← d’abord S5
  ↓
Doc 3  Next + Expo consomment /v1                 ← F1–F3
```

Toute exception (ex. « on branche le mobile sur 8083 pour une démo ») est **hors contrat produit** et ne doit pas entrer en production.
