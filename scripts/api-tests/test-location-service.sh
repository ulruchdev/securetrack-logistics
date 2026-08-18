#!/usr/bin/env bash
# =============================================================
#  Tests API - LocationService (points de transition + Feign)
#  Prérequis : PackageService démarré (port 8081)
#  Usage : bash test-location-service.sh
# =============================================================
set -uo pipefail
cd -- "$(dirname "$0")" || { echo "ERREUR : impossible de changer de répertoire" >&2; exit 1; }
source ./lib.sh
[ -f ./config.env ] && source ./config.env

echo "===== LOCATION-SERVICE ($BASE_LOC) ====="

# --- 0. Préparation : création d'un colis (valeur auto-générée) ---
NAME=$(gen_name)
R=$(req POST "$BASE_PKG/api/packages" "{\"description\":\"Colis pour location\",\"packageName\":\"$NAME\",\"packageType\":\"Informatique\",\"weight\":1.2,\"fragile\":false}")
PKG_ID=$(jval "$(resp_body "$R")" packageId)
echo "       -> packageId utilisé : $PKG_ID"

# --- 1. Création valide (package existant + checkpoint dispo) ---
R=$(req POST "$BASE_LOC/api/locations" "{\"packageId\":$PKG_ID,\"city\":\"Paris\",\"zone\":\"Zone Nord\",\"checkpointAvailable\":true}")
check "POST /api/locations (creation valide)" 201 "$(resp_code "$R")"
LOC_ID=$(jval "$(resp_body "$R")" locationId)
echo "       -> locationId extrait automatiquement : $LOC_ID"

# --- 2. Création avec package INEXISTANT ---
R=$(req POST "$BASE_LOC/api/locations" '{"packageId":999999,"city":"Lyon","zone":"Zone Sud","checkpointAvailable":false}')
# NOTE: comportement actuel = 500 (IllegalArgumentException non mappé).
#       Idéalement devrait être 400. Voir README (anomalies connues).
check "POST /api/locations (package inexistant)" 500 "$(resp_code "$R")"

# --- 3. Lecture par id ---
R=$(req GET "$BASE_LOC/api/locations/$LOC_ID")
check "GET /api/locations/$LOC_ID" 200 "$(resp_code "$R")"

# --- 4. Pagination ---
R=$(req GET "$BASE_LOC/api/locations?page=0&size=5")
check "GET /api/locations (pagination)" 200 "$(resp_code "$R")"

# --- 5. Location enrichie par package (chaîne Feign) ---
R=$(req GET "$BASE_LOC/api/locations/by-package/$PKG_ID")
check "GET /api/locations/by-package/$PKG_ID (enrichi)" 200 "$(resp_code "$R")"

# --- 6. Lecture id inexistant ---
R=$(req GET "$BASE_LOC/api/locations/ffffffffffffffffffffffff")
check "GET /api/locations/inexistant" 404 "$(resp_code "$R")"

summary
exit $?
