# Migration MongoDB → PostgreSQL

## Contexte

Le Sprint S2 a introduit un nouveau modèle SQL (Site/Checkpoint) dans le location-service,
en remplacement de l'ancien modèle MongoDB (Location). Ce script migre les données existantes.

## Mapping

| MongoDB (ancien) | PostgreSQL (nouveau) | Règle |
|---|---|---|
| `Location.city` | `sites.name` | Direct |
| `Location.zone` | `sites.address` | `Zone {zone}` |
| `Location.tenantId` | `sites.tenant_id` | Identique |
| `Location.checkpointAvailable=true` | `checkpoints` (nouvelle ligne) | Crée un Checkpoint lié au Site |
| `Location.packageId` | — | **Abandonné** (plus de lien direct) |
| `Location.locationId` | — | **Abandonné** (ID auto-généré par BIGSERIAL) |

Pour les packages :
| PostgreSQL (ancien) | PostgreSQL (nouveau) | Règle |
|---|---|---|
| `package.tracking_number=NULL` | `package.tracking_number=ST-XXXXXXXX` | Généré aléatoirement |

## Prérequis

```bash
# Installer les dépendances Python
pip install -r scripts/migration/requirements.txt

# Variables d'environnement obligatoires
export PG_PASSWORD=cbspassword
export MONGO_URI=mongodb://cbsuser:cbspassword@localhost:27017/cbsdb?authSource=admin
```

## Utilisation

### 1. Dry-run (simulation)

```bash
python3 scripts/migration/migrate_mongo_to_sql.py --dry-run
```

### 2. Migration complète

```bash
python3 scripts/migration/migrate_mongo_to_sql.py
```

### 3. Migration par tenant

```bash
python3 scripts/migration/migrate_mongo_to_sql.py --tenant tenant-alpha
```

### 4. Migration partielle

```bash
# Uniquement les locations (pas les packages)
python3 scripts/migration/migrate_mongo_to_sql.py --skip-packages

# Uniquement les packages (pas les locations)
python3 scripts/migration/migrate_mongo_to_sql.py --skip-locations
```

## Vérification post-migration

```sql
-- Vérifier les sites migrés
SELECT COUNT(*) AS total_sites FROM sites;

-- Vérifier les checkpoints créés
SELECT COUNT(*) AS total_checkpoints FROM checkpoints;

-- Vérifier les packages avec trackingNumber
SELECT COUNT(*) AS packages_with_tracking FROM package WHERE tracking_number IS NOT NULL;

-- Vérifier la correspondance tenant
SELECT s.tenant_id, COUNT(*) AS sites
FROM sites s
GROUP BY s.tenant_id;
```

## Rollback

En cas de problème, les tables `sites` et `checkpoints` sont nouvelles et peuvent être vidées :

```sql
TRUNCATE checkpoints CASCADE;
TRUNCATE sites CASCADE;

-- Pour les packages, les trackingNumbers sont générés aléatoirement
-- et n'écrasent aucune donnée existante (UPDATE sur colonne NULL)
```

## Notes

- Le script est **idempotent** : les preConditions Liquibase empêchent les doublons
- Le mapping `checkpointAvailable=true → Checkpoint` crée UN checkpoint par site
- Le `packageId` de l'ancien modèle MongoDB est **abandonné** (nouveau modèle = trackingNumber)
- Les `latitude`/`longitude` ne sont pas disponibles dans MongoDB → NULL dans SQL
