#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-8080}"
MAX_ATTEMPTS="${2:-30}"
SLEEP_SECONDS="${3:-10}"

echo "Waiting for OpenMRS on http://localhost:${PORT}/openmrs ..."

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  if curl -sf "http://localhost:${PORT}/openmrs" > /dev/null; then
    echo "OpenMRS responded successfully (attempt ${attempt}/${MAX_ATTEMPTS})"
    exit 0
  fi
  echo "Attempt ${attempt}/${MAX_ATTEMPTS} failed, retrying in ${SLEEP_SECONDS}s ..."
  sleep "$SLEEP_SECONDS"
done

echo "Smoke test failed: OpenMRS did not become reachable on port ${PORT}"
exit 1
