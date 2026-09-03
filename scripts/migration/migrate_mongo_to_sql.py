#!/usr/bin/env python3
"""
CBS Logistics — Migration MongoDB → PostgreSQL
===============================================

Ce script migre les données du modèle ancien (MongoDB locations)
vers le nouveau modèle SQL (sites + checkpoints).

Mapping :
  MongoDB locations → PostgreSQL sites
  - locationId  → (généré auto par BIGSERIAL)
  - tenantId    → tenantId
  - city        → name
  - zone        → address
  - (latitude/longitude non disponibles → NULL)
  - checkpointAvailable=true → crée un Checkpoint dans la table checkpoints

Prérequis :
  pip install pymongo psycopg2-binary

Usage :
  python3 migrate_mongo_to_sql.py [--dry-run] [--tenant TENANT_ID]

Variables d'environnement :
  MONGO_URI     : URI MongoDB (défaut: mongodb://cbsuser:cbspassword@localhost:27017/cbsdb?authSource=admin)
  DB_URL        : URL PostgreSQL (défaut: jdbc:postgresql://localhost:5433/cbsdb)
  DB_USERNAME   : User PostgreSQL (défaut: cbsuser)
  DB_PASSWORD   : Password PostgreSQL (OBLIGATOIRE)
"""

import os
import sys
import argparse
import logging
from datetime import datetime

try:
    import pymongo
except ImportError:
    print("ERREUR: pymongo non installé. Exécutez: pip install pymongo")
    sys.exit(1)

try:
    import psycopg2
    from psycopg2.extras import execute_values
except ImportError:
    print("ERREUR: psycopg2 non installé. Exécutez: pip install psycopg2-binary")
    sys.exit(1)


# ── Configuration ──────────────────────────────────────────────────────────

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb://cbsuser:cbspassword@localhost:27017/cbsdb?authSource=admin"
)

PG_HOST = os.environ.get("PG_HOST", "localhost")
PG_PORT = int(os.environ.get("PG_PORT", "5433"))
PG_DB = os.environ.get("PG_DB", "cbsdb")
PG_USER = os.environ.get("PG_USER", "cbsuser")
PG_PASSWORD = os.environ.get("PG_PASSWORD", "")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
log = logging.getLogger("migration")


# ── Connexions ─────────────────────────────────────────────────────────────

def connect_mongo(uri: str):
    """Connexion à MongoDB."""
    client = pymongo.MongoClient(uri, serverSelectionTimeoutMS=5000)
    try:
        client.admin.command("ping")
        log.info("✅ Connecté à MongoDB")
    except Exception as e:
        log.error(f"❌ Impossible de se connecter à MongoDB: {e}")
        sys.exit(1)
    return client


def connect_postgres():
    """Connexion à PostgreSQL."""
    if not PG_PASSWORD:
        log.error("❌ PG_PASSWORD est obligatoire")
        sys.exit(1)
    try:
        conn = psycopg2.connect(
            host=PG_HOST, port=PG_PORT,
            dbname=PG_DB, user=PG_USER, password=PG_PASSWORD
        )
        conn.autocommit = False
        log.info("✅ Connecté à PostgreSQL")
        return conn
    except Exception as e:
        log.error(f"❌ Impossible de se connecter à PostgreSQL: {e}")
        sys.exit(1)


# ── Migration Locations → Sites + Checkpoints ──────────────────────────────

def migrate_locations(mongo_db, pg_conn, tenant_filter: str = None, dry_run: bool = False):
    """
    Migre la collection 'locations' (MongoDB) vers les tables 'sites' + 'checkpoints' (PostgreSQL).

    Mapping :
      Location { locationId, tenantId, city, zone, checkpointAvailable }
        → Site { tenantId, name=city, address=zone, active=true }
        → Checkpoint { tenantId, siteId=<nouveau>, name="Checkpoint principal", active=true }
    """
    collection = mongo_db["locations"]
    query = {}
    if tenant_filter:
        query["tenantId"] = tenant_filter

    total = collection.count_documents(query)
    log.info(f"📦 {total} locations trouvées dans MongoDB" + (f" (tenant: {tenant_filter})" if tenant_filter else ""))

    if total == 0:
        log.info("ℹ️  Aucune location à migrer")
        return 0, 0

    cursor = collection.find(query)
    sites_created = 0
    checkpoints_created = 0
    errors = 0

    cur = pg_conn.cursor()

    for doc in cursor:
        try:
            tenant_id = doc.get("tenantId", "default")
            city = doc.get("city", "Unknown")
            zone = doc.get("zone", "")
            checkpoint_available = doc.get("checkpointAvailable", False)

            # 1. Créer le Site
            site_name = f"{city}" if not zone else f"{city} - {zone}"
            site_address = f"Zone {zone}" if zone else None

            if not dry_run:
                cur.execute("""
                    INSERT INTO sites (tenant_id, name, address, active)
                    VALUES (%s, %s, %s, true)
                    RETURNING id
                """, (tenant_id, site_name, site_address))
                site_id = cur.fetchone()[0]
            else:
                site_id = -1  # Placeholder pour dry-run
                log.info(f"  [DRY-RUN] Site: {site_name} (tenant={tenant_id})")

            sites_created += 1

            # 2. Créer le Checkpoint si checkpointAvailable=true
            if checkpoint_available:
                if not dry_run:
                    cur.execute("""
                        INSERT INTO checkpoints (tenant_id, site_id, name, active)
                        VALUES (%s, %s, %s, true)
                    """, (tenant_id, site_id, f"Checkpoint {site_name}"))
                checkpoints_created += 1
                if dry_run:
                    log.info(f"  [DRY-RUN] Checkpoint pour site: {site_name}")

        except Exception as e:
            errors += 1
            log.error(f"  ❌ Erreur migration location {doc.get('_id')}: {e}")
            pg_conn.rollback()
            continue

    if not dry_run:
        pg_conn.commit()
        log.info(f"✅ Migration terminée: {sites_created} sites, {checkpoints_created} checkpoints ({errors} erreurs)")
    else:
        pg_conn.rollback()
        log.info(f"✅ [DRY-RUN] Simulation: {sites_created} sites, {checkpoints_created} checkpoints")

    return sites_created, checkpoints_created


# ── Migration Packages : ajout trackingNumber ──────────────────────────────

def migrate_packages_tracking_numbers(mongo_db, pg_conn, tenant_filter: str = None, dry_run: bool = False):
    """
    Génère des trackingNumbers (ST-XXXXXXXX) pour les packages existants dans PostgreSQL
    qui n'en ont pas encore.
    """
    cur = pg_conn.cursor()

    # Trouver les packages sans trackingNumber
    if tenant_filter:
        cur.execute("""
            SELECT package_id, tenant_id FROM package
            WHERE tracking_number IS NULL AND tenant_id = %s
        """, (tenant_filter,))
    else:
        cur.execute("SELECT package_id, tenant_id FROM package WHERE tracking_number IS NULL")

    packages = cur.fetchall()
    log.info(f"📦 {len(packages)} packages sans trackingNumber trouvés")

    if not packages:
        log.info("ℹ️  Tous les packages ont déjà un trackingNumber")
        return 0

    import random
    import string

    chars = string.ascii_uppercase + string.digits
    updated = 0

    for pkg_id, tenant_id in packages:
        try:
            # Générer un trackingNumber unique
            tracking_number = "ST-" + "".join(random.choices(chars, k=8))

            if not dry_run:
                cur.execute("""
                    UPDATE package SET tracking_number = %s WHERE package_id = %s
                """, (tracking_number, pkg_id))
            else:
                log.info(f"  [DRY-RUN] Package {pkg_id} → {tracking_number}")

            updated += 1
        except Exception as e:
            log.error(f"  ❌ Erreur package {pkg_id}: {e}")
            pg_conn.rollback()
            continue

    if not dry_run:
        pg_conn.commit()
        log.info(f"✅ {updated} packages mis à jour avec trackingNumber")
    else:
        pg_conn.rollback()
        log.info(f"✅ [DRY-RUN] Simulation: {updated} packages")

    return updated


# ── Main ───────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Migration MongoDB → PostgreSQL")
    parser.add_argument("--dry-run", action="store_true", help="Simuler sans écrire")
    parser.add_argument("--tenant", type=str, help="Migrer un seul tenant (optionnel)")
    parser.add_argument("--skip-locations", action="store_true", help="Ignorer la migration locations")
    parser.add_argument("--skip-packages", action="store_true", help="Ignorer la migration packages")
    args = parser.parse_args()

    log.info("=" * 60)
    log.info("CBS Logistics — Migration MongoDB → PostgreSQL")
    log.info("=" * 60)
    if args.dry_run:
        log.info("⚠️  MODE DRY-RUN : aucune modification ne sera effectuée")
    log.info("")

    # Connexions
    mongo_client = connect_mongo(MONGO_URI)
    mongo_db = mongo_client.get_database()
    pg_conn = connect_postgres()

    try:
        # 1. Migration Locations → Sites + Checkpoints
        if not args.skip_locations:
            log.info("── Étape 1/2 : Migration Locations → Sites + Checkpoints ──")
            sites, checkpoints = migrate_locations(
                mongo_db, pg_conn, args.tenant, args.dry_run
            )
            log.info("")

        # 2. Migration Packages : ajout trackingNumber
        if not args.skip_packages:
            log.info("── Étape 2/2 : Migration Packages : ajout trackingNumber ──")
            packages = migrate_packages_tracking_numbers(
                mongo_db, pg_conn, args.tenant, args.dry_run
            )
            log.info("")

        log.info("=" * 60)
        log.info("✅ Migration terminée avec succès")
        log.info("=" * 60)

    except Exception as e:
        log.error(f"❌ Erreur fatale: {e}")
        pg_conn.rollback()
        sys.exit(1)
    finally:
        pg_conn.close()
        mongo_client.close()


if __name__ == "__main__":
    main()
