#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
BASE_URL="${BASE_URL%/}"

ENDPOINTS=(
  "/api/articles"
  "/api/articles/hot?size=10"
  "/api/categories"
  "/api/tags"
)

echo "Warming cache against ${BASE_URL}..."

for endpoint in "${ENDPOINTS[@]}"; do
  url="${BASE_URL}${endpoint}"
  for i in 1 2; do
    status=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept: application/json" "$url")
    if [ "$status" != "200" ]; then
      echo "Request failed: ${url} (HTTP ${status})"
      exit 1
    fi
  done
  echo "OK: ${endpoint}"
done

echo "Cache warmup complete."
