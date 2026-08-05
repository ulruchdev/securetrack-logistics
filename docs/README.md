# CBS Logistics - Documentation API

Ce dossier contient les spécifications OpenAPI 3.0.3 pour tous les microservices du système CBS Logistics.

## Services Disponibles

### 1. Package Service (Port 8081)
- **Fichier**: `package-service-openapi.yaml`
- **Description**: Gestion CRUD complète des colis
- **Fonctionnalités**: Création, consultation, mise à jour, suppression avec pagination et tri

### 2. Location Service (Port 8082)
- **Fichier**: `location-service-openapi.yaml`
- **Description**: Gestion des emplacements logistiques
- **Fonctionnalités**: Création, consultation avec enrichissement via Package Service

### 3. Security Checkpoint Service (Port 8083)
- **Fichier**: `security-checkpoint-service-openapi.yaml`
- **Description**: Traçabilité sécurisée des passages aux checkpoints
- **Fonctionnalités**: Enregistrement et consultation des logs de passage
- **Sécurité**: Basic Authentication (admin:secret123)

## Utilisation

### Swagger UI
Chaque service expose sa documentation interactive :
- Package Service: `http://localhost:8081/swagger-ui/index.html`
- Location Service: `http://localhost:8082/swagger-ui/index.html`
- Security Checkpoint Service: `http://localhost:8083/swagger-ui/index.html`

### Génération de clients
Ces spécifications peuvent être utilisées avec des outils comme :
- OpenAPI Generator
- Swagger Codegen
- Postman (import direct)

### Validation
Les spécifications sont conformes à OpenAPI 3.0.3 et peuvent être validées avec :
```bash
swagger-codegen validate -i package-service-openapi.yaml
```

## Architecture Inter-Services

```
Package Service (8081) ←→ Location Service (8082) ←→ Security Checkpoint Service (8083)
```

- Location Service valide les colis via Package Service
- Security Checkpoint Service valide les locations via Location Service
- Tous les services utilisent PostgreSQL (Package/Security) ou MongoDB (Location)