#!/usr/bin/env python3
"""
Génère docs/combined-openapi.yaml à partir des 3 specs dédiées.

Usage :
    python3 docs/generate-combined-openapi.py

Chaque opération reçoit :
  - son serveur cible (port du service) -> corrige le problème "3 servers à la racine"
  - un tag par service (regroupement Swagger UI)
  - le security requirement si le service source l'exige (Basic Auth)

À régénérer après toute modification d'une spec source.
"""
import sys
from pathlib import Path

import yaml

DOCS = Path(__file__).resolve().parent

SERVICES = [
    ("package-service-openapi.yaml", 8081, "Package Service"),
    ("location-service-openapi.yaml", 8082, "Location Service"),
    ("security-checkpoint-service-openapi.yaml", 8083, "Security Checkpoint Service"),
    ("tracking-service-openapi.yaml", 8084, "Tracking Service"),
]

HEADER = """\
# ============================================================
#  CBS Logistics API - Spec combinée (FICHIER GÉNÉRÉ - ne pas éditer à la main)
#
#  Sources : package-service-openapi.yaml, location-service-openapi.yaml,
#            security-checkpoint-service-openapi.yaml, tracking-service-openapi.yaml
#  Régénération : python3 docs/generate-combined-openapi.py
# ============================================================
"""


def load(name: str) -> dict:
    with open(DOCS / name, encoding="utf-8") as f:
        return yaml.safe_load(f)


def main() -> int:
    paths: dict = {}
    schemas: dict = {}
    security_schemes: dict = {}

    for filename, port, service_name in SERVICES:
        spec = load(filename)
        root_security = spec.get("security", [])

        # --- chemins : servers + tags + security par opération ---
        for path, path_item in spec.get("paths", {}).items():
            new_path_item = {}
            for method, operation in path_item.items():
                if method not in ("get", "post", "put", "patch", "delete", "options", "head", "trace"):
                    new_path_item[method] = operation
                    continue
                op = dict(operation)
                op["servers"] = [{"url": f"http://localhost:{port}", "description": service_name}]
                tags = list(op.get("tags", []))
                if service_name not in tags:
                    op["tags"] = [service_name] + tags
                if root_security:
                    op["security"] = root_security
                new_path_item[method] = op
            if path in paths:
                print(f"AVERTISSEMENT : path dupliqué {path} ({filename}) - fusionné")
                paths[path].update(new_path_item)
            else:
                paths[path] = new_path_item

        # --- composants : schémas + securitySchemes ---
        components = spec.get("components", {})
        security_schemes.update(components.get("securitySchemes", {}))
        for schema_name, schema in components.get("schemas", {}).items():
            if schema_name in schemas and schemas[schema_name] != schema:
                print(f"ERREUR : schéma {schema_name} défini différemment dans {filename}")
                return 1
            schemas[schema_name] = schema

    combined = {
        "openapi": "3.0.3",
        "info": {
            "title": "CBS Logistics API",
            "version": "1.0.0",
            "description": (
                "API combinée pour le système CBS Logistics incluant Package Service, "
                "Location Service, Security Checkpoint Service et Tracking Service."
            ),
        },
        "servers": [{"url": f"http://localhost:{port}", "description": name} for _, port, name in SERVICES],
        "paths": paths,
        "components": {"securitySchemes": security_schemes, "schemas": schemas},
    }

    output = HEADER + yaml.dump(
        combined,
        sort_keys=False,
        default_flow_style=False,
        allow_unicode=True,
        width=120,
    )
    (DOCS / "combined-openapi.yaml").write_text(output, encoding="utf-8")
    print("combined-openapi.yaml régénéré ✅")
    print(f"  - paths : {len(paths)}")
    print(f"  - schémas : {len(schemas)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
