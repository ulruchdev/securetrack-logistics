#!/usr/bin/env bash
# =============================================================
#  CBS Logistics - Test bout-en-bout via Gateway (port 8080)
#  Flux : Site → Checkpoint → Package → Scan → Tracking
#
#  Usage : bash test-gateway-flow.sh
#  Prerequis : infrastructure + tous les services demarres
# =============================================================
set -uo pipefail
cd -- "$(dirname "$0")" || { echo "ERREUR : impossible de changer de repertoire" >&2; exit 1; }
source ./lib.sh

BASE_GW="${BASE_GW:-http://localhost:8080}"

echo
echo "========================================================"
echo "  TEST GATEWAY — Flux complet Site→Checkpoint→Package→Scan→Tracking"
echo "========================================================"

# --- 0. Verifier que le Gateway est up ---
echo
echo "--- Healthcheck Gateway ---"
RESULT=$(req GET "$BASE_GW/actuator/health")
CODE=$(echo "$RESULT" | tail -1)
check "Gateway health" "200" "$CODE"

# --- 1. Creer un Site via Gateway ---
echo
echo "--- 1. Creer un Site ---"
SITE_BODY='{"name":"Site Test Gateway","address":"100 rue du Test","latitude":48.85,"longitude":2.35}'
RESULT=$(req POST "$BASE_GW/api/sites" "$SITE_BODY")
CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | sed '$d')
check "Creer site" "201" "$CODE"
SITE_ID=$(echo "$BODY" | json_val "id")
echo "  Site ID : $SITE_ID"

# --- 2. Creer un Checkpoint sur le Site ---
echo
echo "--- 2. Creer un Checkpoint ---"
CHK_BODY="{\"siteId\":$SITE_ID,\"name\":\"Gate Test\"}"
RESULT=$(req POST "$BASE_GW/api/checkpoints" "$CHK_BODY")
CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | sed '$d')
check "Creer checkpoint" "201" "$CODE"
CHK_ID=$(echo "$BODY" | json_val "id")
echo "  Checkpoint ID : $CHK_ID"

# --- 3. Creer un Colis via Gateway ---
echo
echo "--- 3. Creer un Colis ---"
PKG_BODY="{\"packageName\":\"Colis Test\",\"packageType\":\"standard\",\"description\":\"E2E test\"}"
RESULT=$(req POST "$BASE_GW/api/packages" "$PKG_BODY")
CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | sed '$d')
check "Creer colis" "201" "$CODE"
PKG_ID=$(echo "$BODY" | json_val "id")
TRACKING=$(echo "$BODY" | json_val "trackingNumber")
echo "  Package ID : $PKG_ID"
echo "  Tracking Number : $TRACKING"

# --- 4. Enregistrer un scan au checkpoint ---
echo
echo "--- 4. Scan au checkpoint ---"
SCAN_BODY="{\"packageId\":$PKG_ID,\"checkpointId\":$CHK_ID,\"locationId\":$SITE_ID,\"comment\":\"Scan E2E\"}"
RESULT=$(req POST "$BASE_GW/api/checkpoints/scan" "$SCAN_BODY")
CODE=$(echo "$RESULT" | tail -1)
check "Scan checkpoint" "201" "$CODE"

# --- 5. Verifier l'historique de tracking ---
echo
echo "--- 5. Historique Tracking ---"
RESULT=$(req GET "$BASE_GW/api/tracking/by-package/$PKG_ID")
CODE=$(echo "$RESULT" | tail -1)
check "Historique tracking" "200" "$CODE"

# --- 6. Verifier que le package a bien le trackingNumber ---
echo
echo "--- 6. Verifier Package ---"
RESULT=$(req GET "$BASE_GW/api/packages/$PKG_ID")
CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | sed '$d')
check "Lire package" "200" "$CODE"
RESULT_TRACKING=$(echo "$BODY" | json_val "trackingNumber")
if [ "$RESULT_TRACKING" = "$TRACKING" ]; then
  PASS=$((PASS+1))
  echo "  [OK]   Tracking number coherent : $RESULT_TRACKING"
else
  FAIL=$((FAIL+1))
  FAILURES+=("Tracking number incoherent (attendu $TRACKING, recu $RESULT_TRACKING)")
  echo "  [FAIL] Tracking number incoherent (attendu $TRACKING, recu $RESULT_TRACKING)"
fi

# --- 7. Lister les checkpoints du site ---
echo
echo "--- 7. Lister checkpoints du site ---"
RESULT=$(req GET "$BASE_GW/api/checkpoints/by-site/$SITE_ID")
CODE=$(echo "$RESULT" | tail -1)
check "Lister checkpoints" "200" "$CODE"

summary
