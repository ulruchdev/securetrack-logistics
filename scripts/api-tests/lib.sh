#!/usr/bin/env bash
# =============================================================
#  CBS Logistics - Bibliothèque partagée pour les tests API
#  Chargée par chaque script de test (source ./lib.sh)
# =============================================================

# --- Configuration par défaut (surchargée par config.env) ---
BASE_PKG="${BASE_PKG:-http://localhost:8081}"
BASE_LOC="${BASE_LOC:-http://localhost:8082}"
BASE_CHK="${BASE_CHK:-http://localhost:8083}"

AUTH_USER="${AUTH_USER:-admin}"
# AUTH_PASS est OBLIGATOIRE pour les appels authentifiés :
# aucune valeur par défaut n'est codée en dur (vérifié dans req_auth).

# --- Timeouts curl (bornent chaque requête, évite les blocages) ---
CURL_TIMEOUT="${CURL_TIMEOUT:-20}"      # temps total (--max-time)
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-5}"  # connexion (--connect-timeout)

# --- Compteurs globaux ---
PASS=0
FAIL=0
FAILURES=()

# ---------------------------------------------------------------
#  Assertion HTTP : check <description> <code_attendu> <code_recu>
# ---------------------------------------------------------------
check() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    PASS=$((PASS+1))
    echo "  [OK]   $desc  -> HTTP $actual"
  else
    FAIL=$((FAIL+1))
    FAILURES+=("$desc (attendu $expected, recu $actual)")
    echo "  [FAIL] $desc  -> HTTP $actual (attendu $expected)"
  fi
}

# ---------------------------------------------------------------
#  Requêtes HTTP : renvoient "body\nHTTP_CODE" (séparées par \n)
# ---------------------------------------------------------------
req() { # req <method> <url> <data JSON ou vide>
  local method="$1" url="$2" data="${3:-}"
  if [ -n "$data" ]; then
    curl -s --connect-timeout "$CURL_CONNECT_TIMEOUT" --max-time "$CURL_TIMEOUT" \
      -w '\n%{http_code}' -X "$method" "$url" -H 'Content-Type: application/json' -d "$data"
  else
    curl -s --connect-timeout "$CURL_CONNECT_TIMEOUT" --max-time "$CURL_TIMEOUT" \
      -w '\n%{http_code}' -X "$method" "$url"
  fi
}

req_auth() { # req_auth <method> <url> <data JSON ou vide>  (Basic Auth)
  local method="$1" url="$2" data="${3:-}"
  if [ -z "${AUTH_PASS:-}" ]; then
    echo "ERREUR : AUTH_PASS n'est pas defini (mot de passe Basic Auth du checkpoint)." >&2
    echo "         Exportez-le avant de lancer le script : export AUTH_PASS=<mot-de-passe>" >&2
    exit 1
  fi
  if [ -n "$data" ]; then
    curl -s --connect-timeout "$CURL_CONNECT_TIMEOUT" --max-time "$CURL_TIMEOUT" \
      -w '\n%{http_code}' -X "$method" "$url" -u "$AUTH_USER:$AUTH_PASS" -H 'Content-Type: application/json' -d "$data"
  else
    curl -s --connect-timeout "$CURL_CONNECT_TIMEOUT" --max-time "$CURL_TIMEOUT" \
      -w '\n%{http_code}' -X "$method" "$url" -u "$AUTH_USER:$AUTH_PASS"
  fi
}

resp_body() { echo "$1" | sed '$d'; }   # corps (tout sauf la dernière ligne)
resp_code() { echo "$1" | tail -n1; }   # code HTTP (dernière ligne)

# ---------------------------------------------------------------
#  Extraction de valeur JSON (clé de premier niveau)
#  jval <json> <cle>  ->  imprime la valeur ('' si absente)
# ---------------------------------------------------------------
jval() {
  local json="$1" key="$2"
  echo "$json" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    v = d.get('$key')
    print(v if v is not None else '')
except Exception:
    print('')
" 2>/dev/null
}

# ---------------------------------------------------------------
#  Génération automatique de valeurs de test
# ---------------------------------------------------------------
gen_name()    { echo "Colis-$RANDOM-$RANDOM"; }
gen_comment() { echo "Test automatique $(date +%s)"; }

# ---------------------------------------------------------------
#  Bilan du script : summary  ->  renvoie 0 si aucun échec
# ---------------------------------------------------------------
summary() {
  echo
  echo "================================================"
  echo "  RESULTAT : $PASS reussis, $FAIL echoues"
  if [ "$FAIL" -gt 0 ]; then
    echo "  Echecs :"
    for f in "${FAILURES[@]}"; do echo "    - $f"; done
  fi
  echo "================================================"
  [ "$FAIL" -eq 0 ]
}
