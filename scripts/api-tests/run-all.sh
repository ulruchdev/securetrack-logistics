#!/usr/bin/env bash
# =============================================================
#  CBS Logistics - Exécution de toute la suite de tests API
#  Usage : bash run-all.sh
# =============================================================
set -uo pipefail
cd -- "$(dirname "$0")" || { echo "ERREUR : impossible de changer de répertoire" >&2; exit 1; }

OK=0
KO=0

for script in test-package-service.sh test-location-service.sh test-security-checkpoint-service.sh; do
  echo
  echo "########################################################"
  echo "###  $script"
  echo "########################################################"
  if bash "$script"; then
    OK=$((OK+1))
  else
    KO=$((KO+1))
  fi
done

echo
echo "========================================================"
echo "  SUITE COMPLETE : $OK/3 scripts OK, $KO/3 en echec"
echo "========================================================"
[ "$KO" -eq 0 ]
