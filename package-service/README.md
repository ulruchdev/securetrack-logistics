# CBS Logistics - Package Service

## Description métier

Le Package Service est un microservice RESTful responsable de la gestion complète du cycle de vie des colis dans le système de logistique CBS. Il permet la création, consultation, mise à jour partielle et suppression des colis, avec un suivi rigoureux des statuts (NOUVEAU → EN_TRANSIT → LIVRÉ ou PERDU).

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Docker et Docker Compose** pour la base de données PostgreSQL (utilise le `docker-compose.yml` commun)

## Lancement du service

1. **Démarrer la base de données PostgreSQL :**
   ```bash
   cd common
   docker-compose up -d
   ```

2. **Lancer l'application :**
   ```bash
   cd package-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

> 💡 **Schéma de base de données** : géré par **Liquibase** (migrations versionnées dans `db/changelog/`).
> `ddl-auto` est en `none` dans tous les profils — chaque évolution de schéma passe par un changelog.

L'application sera accessible sur `http://localhost:8081`.

## Endpoints API

### Créer un colis
```bash
curl -X POST http://localhost:8081/api/packages \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Colis fragile - Électronique",
    "packageName": "Ordinateur portable",
    "packageType": "Électronique",
    "weight": 2.5,
    "fragile": true
  }'
```

### Récupérer tous les colis (avec pagination)
```bash
curl -X GET "http://localhost:8081/api/packages?page=0&size=10&sortBy=packageName&sortDir=desc"
```

### Récupérer un colis par ID
```bash
curl -X GET http://localhost:8081/api/packages/1
```

### Mettre à jour un colis (mise à jour partielle)
```bash
curl -X PATCH http://localhost:8081/api/packages/1 \
  -H "Content-Type: application/json" \
  -d '{
    "packageStatus": "IN_TRANSIT"
  }'
```

### Supprimer un colis
```bash
curl -X DELETE http://localhost:8081/api/packages/1
```

## Pagination et Tri

La pagination utilise les paramètres suivants :
- `page` : numéro de page (défaut : 0)
- `size` : nombre d'éléments par page (défaut : 10)
- `sortBy` : champ de tri (défaut : packageId)
- `sortDir` : direction du tri - asc/desc (défaut : asc)

Exemple : `?page=1&size=5&sortBy=weight&sortDir=desc`

## Gestion des erreurs

Toutes les erreurs sont renvoyées au format **RFC 7807** (`application/problem+json`), avec les champs `type`, `title`, `status`, `detail` et `instance`, plus `fieldErrors` pour les erreurs de validation.

| Statut | Cas |
|---|---|
| 400 | Validation Bean, paramètre invalide (`sortBy`, `sortDir`, id non numérique), body malformé |
| 404 | Colis non trouvé |
| 409 | Mise à jour concurrente (verrou optimiste @Version) |
| 500 | Erreur interne inattendue |

## Tests

```bash
mvn verify   # exécute les tests + le contrôle JaCoCo (seuil 80 %)
```

## Statut de santé

Vérifiez la santé du service :
```bash
curl http://localhost:8081/actuator/health
```

## Documentation API

La documentation OpenAPI est disponible via Swagger UI :
- **Swagger UI** : `http://localhost:8081/swagger-ui/index.html`
- **OpenAPI JSON** : `http://localhost:8081/v3/api-docs`

## Architecture

- **Framework** : Spring Boot 3.4.1
- **Base de données** : PostgreSQL
- **ORM** : JPA/Hibernate
- **Validation** : Jakarta Validation
- **Mapping** : MapStruct
- **Documentation** : OpenAPI 3.0
