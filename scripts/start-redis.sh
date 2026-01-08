#!/usr/bin/env bash
set -euo pipefail

NETWORK_NAME="${REDIS_NETWORK:-blog-net}"
CONTAINER_NAME="${REDIS_CONTAINER:-blog-redis}"
IMAGE="${REDIS_IMAGE:-redis:7}"
HOST_PORT="${REDIS_PORT:-6379}"
CONTAINER_PORT="${REDIS_CONTAINER_PORT:-6379}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Please install Docker or start Redis manually."
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

if docker ps -a --format '{{.Names}}' | grep -Fxq "$CONTAINER_NAME"; then
  if docker ps --format '{{.Names}}' | grep -Fxq "$CONTAINER_NAME"; then
    echo "Redis already running: ${CONTAINER_NAME}"
  else
    docker start "$CONTAINER_NAME" >/dev/null
    echo "Redis started: ${CONTAINER_NAME}"
  fi
else
  docker run -d \
    --name "$CONTAINER_NAME" \
    --network "$NETWORK_NAME" \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    "$IMAGE" >/dev/null
  echo "Redis container created: ${CONTAINER_NAME}"
fi

echo "Redis is available at localhost:${HOST_PORT}"
