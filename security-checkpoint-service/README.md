# CBS Logistics - Security Checkpoint Service

## Description métier

Le Security Checkpoint Service est un microservice RESTful responsable du contrôle et de l'enregistrement des passages sécurisés des colis à travers les points de contrôle (checkpoints) des localisations. Il garantit la traçabilité sécurisée dans la chaîne logistique CBS.

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Docker et Docker Compose** pour PostgreSQL (utilise le `docker-compose.yml` commun)
- **Location Service** doit être démarré sur le port 8082

## Lancement du service

1. **Démarrer PostgreSQL :**
   ```bash
   cd common
   docker compose up -d
   ```

2. **Démarrer le Location Service :**
   ```bash
   cd project-1-logistics/location-service
   mvn spring-boot:run
   ```

3. **Lancer l'application :**
   ```bash
   cd project-1-logistics/security-checkpoint-service
   mvn spring-boot:run
   ```

L'application sera accessible sur `http://localhost:8083`.

## Sécurité

Le service utilise **Basic Authentication** :
- **Utilisateur** : `admin`
- **Mot de passe** : `secret123`
- **Rôle** : `CHECKPOINT_OPERATOR`

## Endpoints API

### Enregistrer un passage
```bash
curl -u admin:secret123 -X POST http://localhost:8083/api/checkpoints \
  -H "Content-Type: application/json" \
  -d '{
    "packageId": 1,
    "locationId": "507f1f77bcf86cd799439011",
    "result": "OK",
    "comment": "Contrôle réussi",
    "createdBy": "agent001"
  }'
```

### Consulter un log
```bash
curl -u admin:secret123 -X GET http://localhost:8083/api/checkpoints/1
```

### Lister tous les logs (avec pagination)
```bash
curl -u admin:secret123 -X GET "http://localhost:8083/api/checkpoints?page=0&size=10"
```

### Logs par colis (traçabilité)
```bash
curl -u admin:secret123 -X GET http://localhost:8083/api/checkpoints/by-package/1
```

## Modèle de données

### CheckpointLog
```json
{
  "id": 1,
  "packageId": 1,
  "locationId": "507f1f77bcf86cd799439011",
  "checkpointTime": "2024-01-15T10:30:00",
  "result": "OK",
  "comment": "Contrôle réussi",
  "createdBy": "agent001"
}
```

### Résultats possibles
- `OK` : Contrôle réussi
- `REFUSED` : Colis refusé
- `ALERT` : Alerte détectée
- `PENDING` : En attente de validation

## Architecture

- **Framework** : Spring Boot 3.4.1
- **Base de données** : PostgreSQL
- **ORM** : JPA/Hibernate
- **Sécurité** : Spring Security (Basic Auth)
- **Validation** : Jakarta Validation
- **Mapping** : MapStruct
- **Client HTTP** : OpenFeign
- **Documentation** : OpenAPI 3.0

## Documentation API

La documentation OpenAPI est disponible via Swagger UI :
- **Swagger UI** : `http://localhost:8083/swagger-ui/index.html`
- **OpenAPI JSON** : `http://localhost:8083/v3/api-docs`

## Gestion des erreurs

Le service gère les erreurs suivantes :
- **404 NOT_FOUND** : Log de checkpoint non trouvé
- **400 BAD_REQUEST** : Erreurs de validation
- **503 SERVICE_UNAVAILABLE** : Location Service indisponible
- **500 INTERNAL_SERVER_ERROR** : Erreurs internes

## Dépendances externes

- **Location Service** : Validation que la localisation existe et autorise les checkpoints

## Tests

Exécuter les tests unitaires :
```bash
mvn test
```

## Configuration Feign

Le client Feign est configuré dans `application.yml` :
- **Timeout de connexion** : 5000ms
- **Timeout de lecture** : 10000ms
- **Niveau de log** : basic