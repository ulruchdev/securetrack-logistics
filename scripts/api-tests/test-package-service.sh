#!/usr/bin/env bash
# =============================================================
#  Tests API - PackageService (CRUD colis + règles métier)
#  Usage : bash test-package-service.sh
# =============================================================
set -uo pipefail
cd -- "$(dirname "$0")" || { echo "ERREUR : impossible de changer de répertoire" >&2; exit 1; }
source ./lib.sh
[ -f ./config.env ] && source ./config.env

echo "===== PACKAGE-SERVICE ($BASE_PKG) ====="

# --- 1. Création valide (valeurs auto-générées) ---
NAME=$(gen_name)
R=$(req POST "$BASE_PKG/api/packages" "{\"description\":\"Colis fragile - test auto\",\"packageName\":\"$NAME\",\"packageType\":\"Electronique\",\"weight\":2.5,\"fragile\":true}")
check "POST /api/packages (creation valide)" 201 "$(resp_code "$R")"
PKG_ID=$(jval "$(resp_body "$R")" packageId)
echo "       -> packageId extrait automatiquement : $PKG_ID"

# --- 2. Création invalide (validation Jakarta) ---
R=$(req POST "$BASE_PKG/api/packages" '{"description":""}')
check "POST /api/packages (validation echouee)" 400 "$(resp_code "$R")"

# --- 3. Lecture par id ---
R=$(req GET "$BASE_PKG/api/packages/$PKG_ID")
check "GET /api/packages/$PKG_ID" 200 "$(resp_code "$R")"

# --- 4. Lecture id inexistant ---
R=$(req GET "$BASE_PKG/api/packages/999999")
check "GET /api/packages/999999 (inexistant)" 404 "$(resp_code "$R")"

# --- 5. Pagination + tri ---
R=$(req GET "$BASE_PKG/api/packages?page=0&size=5&sortBy=packageName&sortDir=desc")
check "GET /api/packages (pagination + tri)" 200 "$(resp_code "$R")"

# --- 6. Mise à jour partielle NEW -> IN_TRANSIT ---
R=$(req PATCH "$BASE_PKG/api/packages/$PKG_ID" '{"packageStatus":"IN_TRANSIT"}')
check "PATCH /api/packages/$PKG_ID (NEW->IN_TRANSIT)" 200 "$(resp_code "$R")"

# --- 7. Transition de statut INVALIDE (DELIVERED -> NEW) ---
R=$(req PATCH "$BASE_PKG/api/packages/$PKG_ID" '{"packageStatus":"DELIVERED"}')
R=$(req PATCH "$BASE_PKG/api/packages/$PKG_ID" '{"packageStatus":"NEW"}')
# NOTE: comportement actuel = 500 (IllegalArgumentException non mappé).
#       Idéalement devrait être 400. Voir README (anomalies connues).
check "PATCH transition invalide (rejet)" 500 "$(resp_code "$R")"

# --- 8. Suppression ---
R=$(req DELETE "$BASE_PKG/api/packages/$PKG_ID")
check "DELETE /api/packages/$PKG_ID" 204 "$(resp_code "$R")"

# --- 9. Lecture après suppression ---
R=$(req GET "$BASE_PKG/api/packages/$PKG_ID")
check "GET /api/packages/$PKG_ID (apres suppression)" 404 "$(resp_code "$R")"

# --- 10. Health (Actuator) ---
R=$(req GET "$BASE_PKG/actuator/health")
check "GET /actuator/health" 200 "$(resp_code "$R")"

summary
exit $?
