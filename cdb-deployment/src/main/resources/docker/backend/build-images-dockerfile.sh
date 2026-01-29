#!/usr/bin/env bash
# Build Docker images for all Spring Boot apps using per‑module Dockerfiles
# - Each module must contain its own Dockerfile (multi-stage: Maven builder → JRE runtime)
# - Same runtime config/env defaults are provided via build args
# - Works from the monorepo root so multi-module Maven projects build correctly
#
# Usage:
#   ./build-images-dockerfile.sh [version] [--push] [--parallel]
#
# Options/Env:
#   version      Image tag version (default: 1.0.0)
#   --push       Push images to registry after build
#   --parallel   Build images in parallel
#   REGISTRY     Optional registry prefix (e.g., ghcr.io/your-org)
#   MAX_JOBS     Max parallel jobs (default: 4)
#   MAVEN_IMAGE  Maven builder image (default: maven:3.9-eclipse-temurin-21)
#   RUNTIME_IMAGE Runtime base (default: eclipse-temurin:21-jre)
#   LOCAL_M2     Path to local Maven repo mount for pre-install (default: $REPO_ROOT/.m2)
#
set -euo pipefail

log() { echo "[build-images-dockerfile] $*"; }
err() { echo "[build-images-dockerfile][ERROR] $*" >&2; }

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
MAVEN_IMAGE="${MAVEN_IMAGE:-maven:3.9-eclipse-temurin-21}"
RUNTIME_IMAGE="${RUNTIME_IMAGE:-eclipse-temurin:21-jre}"

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

# Optional repo-scoped Maven cache for pre-install
LOCAL_M2="${LOCAL_M2:-$REPO_ROOT/.m2}"
mkdir -p "$LOCAL_M2"
log "Using Maven local repository at: $LOCAL_M2"

# ---------- Modules to build ----------
# Format: "modulePath:imageBaseName"
DOCKER_MODULES=(
  "cdb-api-gateway:cdb/cdb-api-gateway"
  "cdb-provider-registry:cdb/cdb-provider-registry"
  "cdb-api-registry:cdb/cdb-api-registry"
  "cdb-auth-registry:cdb/cdb-auth-registry"
  "cdb-master-data-engine:cdb/cdb-master-data-engine"
)

image_full_name() { [[ -n "$REGISTRY" ]] && echo "$REGISTRY/$1:$2" || echo "$1:$2"; }

# ---------- Pre-install internal modules (optional but recommended) ----------
log "Installing internal modules (shared libraries) first..."
(
  cd "$REPO_ROOT"
  docker run --rm \
    -v "$REPO_ROOT":/workspace \
    -v "$LOCAL_M2":/root/.m2 \
    -w /workspace \
    "$MAVEN_IMAGE" \
    mvn -B -DskipTests -Drevision="$VERSION" -Dchangelist= \
      -pl cdb-shared-libraries/cdb-common-core,cdb-shared-libraries/cdb-security-lib -am clean install \
    | sed "s/^/[Maven-INTERNAL] /"
) || { err "Internal modules install failed"; exit 1; }
log "✓ Internal modules installed"

## No common Dockerfile fallback: each module's Dockerfile will be used.

build_image() {
  local entry="$1"
  local module="${entry%%:*}"
  local baseImage="${entry#*:}"
  local fullImage
  fullImage="$(image_full_name "$baseImage" "$VERSION")"

  local module_dockerfile="$REPO_ROOT/$module/Dockerfile"
  if [[ ! -f "$module_dockerfile" ]]; then
    err "Missing Dockerfile for module: $module ($module_dockerfile). This script no longer uses a temporary/common Dockerfile. Please add a Dockerfile to the module."
    return 1
  fi

  log "Building (module Dockerfile) image: $module -> $fullImage"
  (
    cd "$REPO_ROOT"
    # Build args (include proxy vars if present)
    BUILD_ARGS=(
      -f "$module_dockerfile"
      --build-arg MAVEN_IMAGE="$MAVEN_IMAGE"
      --build-arg RUNTIME_IMAGE="$RUNTIME_IMAGE"
      --build-arg VERSION="$VERSION"
    )
    [[ -n "${HTTP_PROXY:-}" ]]  && BUILD_ARGS+=(--build-arg HTTP_PROXY="$HTTP_PROXY")
    [[ -n "${HTTPS_PROXY:-}" ]] && BUILD_ARGS+=(--build-arg HTTPS_PROXY="$HTTPS_PROXY")
    [[ -n "${NO_PROXY:-}" ]]   && BUILD_ARGS+=(--build-arg NO_PROXY="$NO_PROXY")
    [[ -n "${http_proxy:-}" ]]  && BUILD_ARGS+=(--build-arg http_proxy="$http_proxy")
    [[ -n "${https_proxy:-}" ]] && BUILD_ARGS+=(--build-arg https_proxy="$https_proxy")
    [[ -n "${no_proxy:-}" ]]    && BUILD_ARGS+=(--build-arg no_proxy="$no_proxy")

    docker build \
      --network=host \
      "${BUILD_ARGS[@]}" \
      -t "$fullImage" \
      . \
      | sed "s/^/[$module] /"
  ) || { err "Docker build failed for $module"; return 1; }

  if [[ "$PUSH" == "true" ]]; then
    log "Pushing $fullImage"
    docker push "$fullImage" || { err "Push failed for $fullImage"; return 1; }
  fi

  log "✓ Completed: $fullImage"
}

log "Building Docker images using per-module Dockerfiles..."
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
    wait "$pid" || { err "Parallel Docker builds failed"; exit 1; }
  done
else
  for entry in "${DOCKER_MODULES[@]}"; do
    build_image "$entry" || exit 1
  done
fi

log "✓ All Docker images built with tag: $VERSION"
[[ "$PUSH" == "true" ]] && log "✓ All images pushed"
log "Done."
