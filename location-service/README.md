# CBS Logistics - Location Service

## Description métier

Le Location Service est un microservice RESTful responsable de la gestion des emplacements logistiques dans le système CBS. Il permet de créer et consulter des locations avec des informations sur la ville, la zone et la disponibilité des points de contrôle. Le service communique avec le Package Service via Feign Client pour valider et enrichir les données.

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Docker et Docker Compose** pour MongoDB (utilise le `docker-compose.yml` commun)
- **Package Service** doit être démarré sur le port 8081

## Lancement du service

1. **Démarrer MongoDB :**
   ```bash
   cd common
   docker compose up -d
   ```

2. **Démarrer le Package Service :**
   ```bash
   cd project-1-logistics/package-service
   mvn spring-boot:run
   ```

3. **Lancer l'application :**
   ```bash
   cd project-1-logistics/location-service
   mvn spring-boot:run
   ```

L'application sera accessible sur `http://localhost:8082`.

## Endpoints API

### Créer une location
```bash
curl -X POST http://localhost:8082/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "city": "Paris",
    "zone": "Zone Nord",
    "checkpointAvailable": true,
    "packageId": 1
  }'
```

### Récupérer toutes les locations (avec pagination)
```bash
curl -X GET "http://localhost:8082/api/locations?page=0&size=10"
```

### Récupérer une location par ID
```bash
curl -X GET http://localhost:8082/api/locations/507f1f77bcf86cd799439011
```

### Récupérer une location enrichie par Package ID
```bash
curl -X GET http://localhost:8082/api/locations/by-package/1
```

## Pagination

La pagination utilise les paramètres suivants :
- `page` : numéro de page (défaut : 0)
- `size` : nombre d'éléments par page (défaut : 20)

Exemple : `?page=1&size=5`

## Architecture

- **Framework** : Spring Boot 3.4.1
- **Base de données** : MongoDB
- **ODM** : Spring Data MongoDB
- **Validation** : Jakarta Validation
- **Mapping** : MapStruct
- **Client HTTP** : OpenFeign
- **Communication** : Feign Client vers Package Service (port 8081)

## Modèle de données

### Location
```json
{
  "locationId": "507f1f77bcf86cd799439011",
  "city": "Paris",
  "zone": "Zone Nord",
  "checkpointAvailable": true,
  "packageId": 1
}
```

### EnrichedLocationDto
Combine les informations de location avec les détails du colis associé :
```json
{
  "location": {
    "city": "Paris",
    "zone": "Zone Nord",
    "checkpointAvailable": true
  },
  "packageInfo": {
    "packageId": 1,
    "description": "Colis fragile",
    "packageName": "Ordinateur",
    "packageType": "Électronique",
    "weight": 2.5,
    "fragile": true,
    "packageStatus": "NEW"
  }
}
```

## Tests

Exécuter les tests unitaires :
```bash
mvn test
```

Exécuter les tests avec couverture :
```bash
mvn test jacoco:report
```

## Configuration Feign

Le client Feign est configuré dans `application.yml` :
- **Timeout de connexion** : 5000ms
- **Timeout de lecture** : 10000ms
- **Niveau de log** : basic

## Gestion des erreurs

Le service gère les erreurs suivantes :
- **404 NOT_FOUND** : Location ou Package non trouvé
- **400 BAD_REQUEST** : Erreurs de validation
- **503 SERVICE_UNAVAILABLE** : Package Service indisponible
- **500 INTERNAL_SERVER_ERROR** : Erreurs internes

## Dépendances externes

- **Package Service** : Validation de l'existence des colis lors de la création de locations
