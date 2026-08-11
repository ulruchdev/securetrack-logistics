# CBS Logistics — Tests API automatisés

Scripts bash pour tester **tous les endpoints** des 3 microservices (mêmes tests que ceux exécutés manuellement en curl, mais automatisés : valeurs auto-générées, injection d'authentification, assertions HTTP).

## Prérequis

- **curl** et **python3** (pour l'extraction JSON)
- Les 3 services démarrés : PackageService (8081), LocationService (8082), SecurityCheckpointService (8083)
- Configuration de l'infra (voir racine du projet) : PostgreSQL + MongoDB

## Utilisation

```bash
# Lancer un seul service
bash test-package-service.sh
bash test-location-service.sh          # nécessite PackageService
bash test-security-checkpoint-service.sh  # nécessite Package + Location

# Lancer toute la suite (exit code ≠ 0 si un test échoue)
bash run-all.sh
```

## Configuration

Tout se configure dans **`config.env`** (ou via variables d'environnement) :

| Variable | Défaut | Rôle |
|---|---|---|
| `BASE_PKG` | `http://localhost:8081` | URL PackageService |
| `BASE_LOC` | `http://localhost:8082` | URL LocationService |
| `BASE_CHK` | `http://localhost:8083` | URL SecurityCheckpointService |
| `AUTH_USER` | `admin` | Utilisateur Basic Auth (checkpoint) |
| `AUTH_PASS` | _(fourni par l'admin / variable d'env)_ | Mot de passe Basic Auth (checkpoint) |

## Fonctionnalités

- **Remplissage automatique des valeurs** : noms de colis générés aléatoirement, commentaires horodatés
- **Injection de token / authentification** : en-tête Basic Auth `$AUTH_USER:$AUTH_PASS` injecté automatiquement sur les endpoints protégés
- **Extraction automatique des IDs** : `packageId`, `locationId`, `id` relus depuis les réponses pour enchaîner les requêtes (chaîne Feign complète : colis → location → checkpoint)
- **Assertions HTTP** : chaque test vérifie le code retour attendu ; bilan final `X réussis / Y échoués` + liste des échecs

## Résultats attendus

| Service | Tests | Résultat |
|---|---|---|
| PackageService | 10 | Tous verts (création, validation 400, lecture, 404, pagination, PATCH statut, suppression 204, health) |
| LocationService | 6 | Tous verts (création, lecture, pagination, enrichissement Feign, 404) |
| SecurityCheckpointService | 9 | Tous verts (401 sans/mauvais auth, création 201, lecture, pagination, traçabilité, 404, health public) |

## ⚠️ Anomalies connues (comportement actuel documenté)

Les tests reflètent le **comportement actuel** des services. Trois cas devraient idéalement renvoyer **400** mais renvoient **500** (les `IllegalArgumentException` ne sont pas mappées par les `GlobalExceptionHandler`) :

1. `PATCH` avec transition de statut invalide (PackageService) → 500 au lieu de 400
2. Création de location avec colis inexistant (LocationService) → 500 au lieu de 400
3. Passage sur location sans checkpoint disponible (SecurityCheckpointService) → 500 au lieu de 400

**Autre anomalie** : le `PATCH /api/packages/{id}` de MapStruct écrase les champs non fournis avec `null` (mise à jour partielle non réellement partielle).

> Ces anomalies figurent également dans `documentation/RESUME-DU-PROJET.md` (section « Points d'attention »).
