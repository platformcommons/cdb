#!/usr/bin/env bash
# Optimized Docker image build for Spring Boot apps
# Handles parent POM + internal modules + Docker images with live logs
#
# Usage:
#   ./build-images.sh [version] [--push] [--parallel]
#
# Options/Env:
#   version    The image tag version (default: 1.0.0)
#   --push     Push images after build
#   --parallel Build Docker images in parallel
#   REGISTRY   Optional registry prefix (e.g., ghcr.io/your-org)
#   MAX_JOBS   Max parallel jobs (default: 4)

set -euo pipefail

log() { echo "[build-images] $*"; }
err() { echo "[build-images][ERROR] $*" >&2; }

VERSION="${1:-}"
PUSH="false"
PARALLEL="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push) PUSH="true"; shift ;;
    --parallel) PARALLEL="true"; shift ;;
    -h|--help) exit 0 ;;
    *) [[ -z "$VERSION" ]] && VERSION="$1"; shift ;;
  esac
done
VERSION="${VERSION:-1.0.0}"
REGISTRY="${REGISTRY:-}"
MAX_JOBS="${MAX_JOBS:-4}"

# Shared buildpack cache image to reuse layers (e.g., JRE) across modules.
# Override by exporting CACHE_IMAGE (set to empty to disable shared cache).
CACHE_IMAGE="${CACHE_IMAGE:-cdb/build-cache:java-21}"
[[ -n "$CACHE_IMAGE" ]] && log "Using shared buildpack cache image: $CACHE_IMAGE" || log "Shared buildpack cache disabled (CACHE_IMAGE empty)"

command -v docker >/dev/null 2>&1 || { err "Docker CLI required"; exit 1; }

# Find top-most Maven repository root (highest directory with a pom.xml)
START_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT=""
CUR_DIR="$START_DIR"
while [[ "$CUR_DIR" != "/" ]]; do
  if [[ -f "$CUR_DIR/pom.xml" ]]; then
    REPO_ROOT="$CUR_DIR"
  fi
  CUR_DIR="$(dirname "$CUR_DIR")"
done
[[ -n "$REPO_ROOT" && -f "$REPO_ROOT/pom.xml" ]] || { err "Cannot locate repository root"; exit 1; }
log "Repository root: $REPO_ROOT"

# Maven local repository directory (configurable)
# Default: repository-scoped .m2 at the repo root. Override by exporting LOCAL_M2=/path/to/.m2
LOCAL_M2="${LOCAL_M2:-$REPO_ROOT/.m2}"
mkdir -p "$LOCAL_M2"
log "Using Maven local repository at: $LOCAL_M2"

MVN_IMAGE="maven:3.9-eclipse-temurin-21"
log "Using Dockerized Maven ($MVN_IMAGE) with JDK 21"

# ---------- Modules ----------
DOCKER_MODULES=(
  "cdb-api-registry:cdb/cdb-api-registry"
  "cdb-auth-registry:cdb/cdb-auth-registry"
  "cdb-api-gateway:cdb/cdb-api-gateway"
  "cdb-provider-registry:cdb/cdb-provider-registry"

)

image_full_name() { [[ -n "$REGISTRY" ]] && echo "$REGISTRY/$1:$2" || echo "$1:$2"; }



# ---------- Step 1: Install internal modules ----------
log "Installing internal modules (shared libraries) first..."
(
  cd "$REPO_ROOT"
  docker run --rm \
    -v "$REPO_ROOT":/workspace \
    -v "$LOCAL_M2":/root/.m2 \
    -w /workspace \
    "$MVN_IMAGE" \
    mvn -B -DskipTests -Drevision="$VERSION" -Dchangelist= \
      -pl cdb-shared-libraries/cdb-common-core,cdb-shared-libraries/cdb-security-lib -am clean install \
    | sed "s/^/[Maven-INTERNAL] /"
) || { err "Internal modules install failed"; exit 1; }
log "✓ Internal modules installed"


# ----------  Build Docker images ----------
build_image() {
  local entry="$1"
  local module="${entry%%:*}"
  local baseImage="${entry#*:}"
  local fullImage
  fullImage="$(image_full_name "$baseImage" "$VERSION")"

  log "Starting Docker image build: $module -> $fullImage"

  (
    cd "$REPO_ROOT/$module"
    docker run --rm \
      -v "$REPO_ROOT":/workspace \
      -v "$LOCAL_M2":/root/.m2 \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -w "/workspace/$module" \
      "$MVN_IMAGE" \
      mvn -DskipTests \
        -Drevision="$VERSION" \
        -Dchangelist= \
        -Dspring-boot.build-image.imageName="$fullImage" \
        -Dspring-boot.build-image.pullPolicy=IF_NOT_PRESENT \
        -Dspring-boot.build-image.cleanCache=false \
        ${CACHE_IMAGE:+-Dspring-boot.build-image.docker.cacheImage="$CACHE_IMAGE"} \
        spring-boot:build-image -Dspring-boot.build-image.publish=false \
        -Dspring-boot.build-image.env=BP_LOG_LEVEL=debug \
        -Dspring-boot.build-image.env=BP_VERBOSE=true \
        ${BP_JVM_VERSION:+-Dspring-boot.build-image.env=BP_JVM_VERSION="$BP_JVM_VERSION"} \
      | sed "s/^/[$module] /"
  ) || { err "Docker image build failed for $module"; return 1; }

  [[ "$PUSH" == "true" ]] && { log "Pushing $fullImage"; docker push "$fullImage" || { err "Push failed for $fullImage"; return 1; }; }

  log "✓ Completed: $fullImage"
}

log "Step 3/3: Building Docker images..."
if [[ "$PARALLEL" == "true" ]]; then
  pids=()
  for entry in "${DOCKER_MODULES[@]}"; do
    while [[ ${#pids[@]} -ge $MAX_JOBS ]]; do
      for i in "${!pids[@]}"; do
        ! kill -0 "${pids[$i]}" 2>/dev/null && wait "${pids[$i]}" && unset 'pids[i]'
      done
      pids=("${pids[@]}")
      sleep 0.5
    done
    build_image "$entry" &
    pids+=($!)
  done
  for pid in "${pids[@]}"; do
    wait "$pid" || { err "Parallel Docker build failed"; exit 1; }
  done
else
  for entry in "${DOCKER_MODULES[@]}"; do
    build_image "$entry" || exit 1
  done
fi

log "✓ All Docker images built with tag: $VERSION"
[[ "$PUSH" == "true" ]] && log "✓ All images pushed"
log "Done."
