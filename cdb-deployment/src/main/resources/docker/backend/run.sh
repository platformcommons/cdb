#!/usr/bin/env bash
set -euo pipefail

# CDB backend build-and-run helper
# - Builds container images for all backend services using Spring Boot buildpacks
# - Starts Docker Compose in detached mode on the `cdb` network
#
# Usage:
#   ./run.sh                 # build and start
#   CDB_API_GATEWAY_PORT=9090 ./run.sh  # override a variable for this run
#
# Requirements:
# - Docker and Docker Compose v2
# - Maven (for spring-boot:build-image)
#
# JWT keys (production):
# - Provide PEM values via env vars before running this script, for example:
#   export CDB_SECURITY_JWT_RSA_PUBLIC_KEY="$(cat /path/to/public.pem)"
#   export CDB_SECURITY_JWT_RSA_PRIVATE_KEY="$(cat /path/to/private.pem)"   # Auth service only
#   export CDB_SECURITY_JWT_KID="key-2025-09"
#
# If you keep keys base64-encoded externally, decode before exporting, e.g.:
#   export CDB_SECURITY_JWT_RSA_PUBLIC_KEY="$(echo "$PUB_B64" | base64 -d)"
#   export CDB_SECURITY_JWT_RSA_PRIVATE_KEY="$(echo "$PRIV_B64" | base64 -d)"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../../.. && pwd)"
cd "$ROOT_DIR"

echo "[run.sh] Building container images via Spring Boot buildpacks..."
mvn -DskipTests -pl cdb-provider-registry -am spring-boot:build-image
mvn -DskipTests -pl cdb-api-registry -am spring-boot:build-image
mvn -DskipTests -pl cdb-auth-registry -am spring-boot:build-image
mvn -DskipTests -pl cdb-api-gateway -am spring-boot:build-image
mvn -DskipTests -pl cdb-master-data-engine -am spring-boot:build-image

echo "[run.sh] Starting Docker Compose in detached mode..."
cd "$ROOT_DIR/cdb-deployment/resources/backend"
docker compose up -d --remove-orphans

echo "[run.sh] Done. Services running on network 'cdb'."
echo "- Gateway:        http://localhost:${CDB_API_GATEWAY_PORT:-8080}"
echo "- Auth Registry:  http://localhost:${CDB_AUTH_REGISTRY_PORT:-8083}"
echo "- Provider Reg.:  http://localhost:${CDB_PROVIDER_REGISTRY_PORT:-8081} (internal only unless you add ports)"
echo "- API Registry:   http://localhost:${CDB_API_REGISTRY_PORT:-8082} (internal only unless you add ports)"
echo "- Master Data:    http://localhost:${CDB_MASTER_DATA_ENGINE_PORT:-8084} (internal only unless you add ports)"
