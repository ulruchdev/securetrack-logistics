# SecureTrack Logistics

Plateforme SaaS de traçabilité sécurisée de colis à haute valeur avec historique infalsifiable (Event Sourcing + Axon Framework).

## Architecture
- Microservices (4 services)
- Clean Architecture + DDD
- Communication : Feign (synchrone) + Axon (événementiel)
- Bases de données : PostgreSQL + MongoDB
- Tracking : CQRS + Event Sourcing

## Structure du projet

securetrack-logistics/
├── documentation/          # Tous les documents du projet
├── services/               # Les 4 microservices
├── docker-compose.yml
├── .github/workflows/      # CI/CD GitHub Actions
└── README.md

## Documentation
- [Vision Produit](documentation/Document_1.2_Business_Vision.md)
- [Cahier des Charges Fonctionnel](documentation/Document_2.0_CdCF.md)
- [Backlog Ultra-Granulaire](documentation/SecureTrack_Backlog.md)

## Technologies
Java 17 • Spring Boot 3 • Maven • PostgreSQL • MongoDB • Axon Framework • Docker

## Comment démarrer
Voir `documentation/` et le guide de développement (à venir).