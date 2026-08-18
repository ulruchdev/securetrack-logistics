#!/usr/bin/env bash
# =============================================================
#  Tests API - SecurityCheckpointService (logs + sécurité)
#  Prérequis : PackageService (8081) + LocationService (8082)
#  Usage : bash test-security-checkpoint-service.sh
# =============================================================
set -uo pipefail
cd -- "$(dirname "$0")" || { echo "ERREUR : impossible de changer de répertoire" >&2; exit 1; }
source ./lib.sh
[ -f ./config.env ] && source ./config.env

echo "===== SECURITY-CHECKPOINT-SERVICE ($BASE_CHK) ====="

# --- 0. Préparation : colis + 2 locations (checkpoint dispo / non dispo) ---
NAME=$(gen_name)
R=$(req POST "$BASE_PKG/api/packages" "{\"description\":\"Colis checkpoint\",\"packageName\":\"$NAME\",\"packageType\":\"Serveur\",\"weight\":5.0,\"fragile\":true}")
PKG_ID=$(jval "$(resp_body "$R")" packageId)

R=$(req POST "$BASE_LOC/api/locations" "{\"packageId\":$PKG_ID,\"city\":\"Marseille\",\"zone\":\"Zone Port\",\"checkpointAvailable\":true}")
LOC_ID=$(jval "$(resp_body "$R")" locationId)

R=$(req POST "$BASE_LOC/api/locations" "{\"packageId\":$PKG_ID,\"city\":\"Lille\",\"zone\":\"Zone Depot\",\"checkpointAvailable\":false}")
LOC_ID_NO=$(jval "$(resp_body "$R")" locationId)
echo "       -> packageId=$PKG_ID  location(dispo)=$LOC_ID  location(non dispo)=$LOC_ID_NO"

# --- 1. Accès sans authentification ---
R=$(req GET "$BASE_CHK/api/checkpoints")
check "GET /api/checkpoints (sans auth)" 401 "$(resp_code "$R")"

# --- 2. Mauvais identifiants ---
AUTH_PASS_SAVE="$AUTH_PASS"; AUTH_PASS="mauvais"
R=$(req_auth GET "$BASE_CHK/api/checkpoints")
check "GET /api/checkpoints (mauvais credentials)" 401 "$(resp_code "$R")"
AUTH_PASS="$AUTH_PASS_SAVE"

# --- 3. Enregistrement d'un passage (auth OK + location dispo) ---
R=$(req_auth POST "$BASE_CHK/api/checkpoints" "{\"packageId\":$PKG_ID,\"locationId\":\"$LOC_ID\",\"result\":\"OK\",\"comment\":\"$(gen_comment)\",\"createdBy\":\"agent001\"}")
check "POST /api/checkpoints (passage valide)" 201 "$(resp_code "$R")"
CP_ID=$(jval "$(resp_body "$R")" id)
echo "       -> checkpoint id extrait automatiquement : $CP_ID"

# --- 4. Passage sur location SANS checkpoint disponible ---
R=$(req_auth POST "$BASE_CHK/api/checkpoints" "{\"packageId\":$PKG_ID,\"locationId\":\"$LOC_ID_NO\",\"result\":\"OK\",\"comment\":\"test\",\"createdBy\":\"agent001\"}")
# NOTE: comportement actuel = 500 (IllegalArgumentException non mappé).
#       Idéalement devrait être 400. Voir README (anomalies connues).
check "POST /api/checkpoints (checkpoint indisponible)" 500 "$(resp_code "$R")"

# --- 5. Lecture par id ---
R=$(req_auth GET "$BASE_CHK/api/checkpoints/$CP_ID")
check "GET /api/checkpoints/$CP_ID" 200 "$(resp_code "$R")"

# --- 6. Pagination ---
R=$(req_auth GET "$BASE_CHK/api/checkpoints?page=0&size=5")
check "GET /api/checkpoints (pagination)" 200 "$(resp_code "$R")"

# --- 7. Logs par colis (traçabilité) ---
R=$(req_auth GET "$BASE_CHK/api/checkpoints/by-package/$PKG_ID")
check "GET /api/checkpoints/by-package/$PKG_ID" 200 "$(resp_code "$R")"

# --- 8. Lecture id inexistant ---
R=$(req_auth GET "$BASE_CHK/api/checkpoints/999999")
check "GET /api/checkpoints/999999 (inexistant)" 404 "$(resp_code "$R")"

# --- 9. Health publique (sans auth) ---
R=$(req GET "$BASE_CHK/actuator/health")
check "GET /actuator/health (public)" 200 "$(resp_code "$R")"

summary
exit $?
