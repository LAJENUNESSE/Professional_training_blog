#!/usr/bin/env bash
set -euo pipefail

NETWORK_NAME="${REDIS_NETWORK:-blog-net}"
REDIS_CONTAINER="${REDIS_CONTAINER:-blog-redis}"
REDIS_HOST="${REDIS_HOST:-$REDIS_CONTAINER}"
REDIS_PORT="${REDIS_PORT:-6379}"
COMMANDER_NAME="${COMMANDER_CONTAINER:-redis-commander}"
COMMANDER_IMAGE="${COMMANDER_IMAGE:-rediscommander/redis-commander}"
COMMANDER_PORT="${COMMANDER_PORT:-8081}"
REDIS_HOSTS="${REDIS_HOSTS:-local:${REDIS_HOST}:${REDIS_PORT}}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Please install Docker or start redis-commander manually."
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "docker daemon is not running."
  exit 1
fi

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network create "$NETWORK_NAME" >/dev/null
  echo "Created docker network: ${NETWORK_NAME}"
fi

if docker ps -a --format '{{.Names}}' | grep -Fxq "$COMMANDER_NAME"; then
  if docker ps --format '{{.Names}}' | grep -Fxq "$COMMANDER_NAME"; then
    echo "redis-commander already running: ${COMMANDER_NAME}"
  else
    docker start "$COMMANDER_NAME" >/dev/null
    echo "redis-commander started: ${COMMANDER_NAME}"
  fi
else
  docker run -d \
    --name "$COMMANDER_NAME" \
    --network "$NETWORK_NAME" \
    -p "${COMMANDER_PORT}:8081" \
    -e "REDIS_HOSTS=${REDIS_HOSTS}" \
    "$COMMANDER_IMAGE" >/dev/null
  echo "redis-commander container created: ${COMMANDER_NAME}"
fi

echo "redis-commander is available at http://localhost:${COMMANDER_PORT}"
echo "REDIS_HOSTS=${REDIS_HOSTS}"
