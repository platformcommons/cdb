#!/usr/bin/env bash
# Build Docker images using host Maven/JDK (no multi-stage Docker build)
# - JARs are built on the host once, reusing your local ~/.m2 cache
# - Each module has a tiny runtime-only Dockerfile (Dockerfile.host)
# - Greatly speeds up rebuilds since dependencies are not re-downloaded in Docker
#
# Usage:
#   ./build-images-host.sh [version] [--push] [--parallel] [--skip-mvn]
#
# Options/Env:
#   version       Image tag version (default: 1.0.0)
#   --push        Push images to registry after build
#   --parallel    Build images in parallel (default jobs: 4 or MAX_JOBS)
#   --skip-mvn    Skip Maven build if JARs are already present in target/
#   REGISTRY      Optional registry prefix (e.g., ghcr.io/your-org)
#   MAX_JOBS      Max parallel jobs (default: 4)
#   MVN_CMD       Maven command to use (default: mvn on PATH; fallback to ./mvnw if wrapper is present)
#   RUNTIME_IMAGE Runtime base (default in Dockerfile.host: eclipse-temurin:21-jre)

set -euo pipefail

log() { echo "[build-images-host] $*"; }
err() { echo "[build-images-host][ERROR] $*" >&2; }

VERSION="${1:-}"
PUSH="false"
PARALLEL="false"
SKIP_MVN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push) PUSH="true"; shift;;
    --parallel) PARALLEL="true"; shift;;
    --skip-mvn) SKIP_MVN="true"; shift;;
    -h|--help)
      echo "Usage: $0 [version] [--push] [--parallel] [--skip-mvn]"; exit 0;;
    *) [[ -z "$VERSION" ]] && VERSION="$1"; shift;;
  esac
done
VERSION="${VERSION:-1.0.0}"
REGISTRY="${REGISTRY:-}"
MAX_JOBS="${MAX_JOBS:-4}"

# Determine repo root (top-most pom.xml)
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

# Modules and image names
DOCKER_MODULES=(
  "cdb-api-gateway:cdb/cdb-api-gateway"
  "cdb-provider-registry:cdb/cdb-provider-registry"
  "cdb-api-registry:cdb/cdb-api-registry"
  "cdb-auth-registry:cdb/cdb-auth-registry"
  "cdb-master-data-engine:cdb/cdb-master-data-engine"
)

image_full_name() { [[ -n "$REGISTRY" ]] && echo "$REGISTRY/$1:$2" || echo "$1:$2"; }

# Choose Maven command
# Priority:
# 1) Respect MVN_CMD if explicitly provided
# 2) Use system mvn if available
# 3) Fallback to ./mvnw ONLY if wrapper files exist
if [[ -n "${MVN_CMD:-}" ]]; then
  : # use provided MVN_CMD
elif command -v mvn >/dev/null 2>&1; then
  MVN_CMD="mvn"
elif [[ -x "$REPO_ROOT/mvnw" && -f "$REPO_ROOT/.mvn/wrapper/maven-wrapper.properties" ]]; then
  MVN_CMD="$REPO_ROOT/mvnw"
else
  err "No Maven found. Install Maven (so 'mvn' is on PATH) or set MVN_CMD explicitly, or ensure Maven Wrapper files exist."
  err "Tried: system 'mvn' and '$REPO_ROOT/mvnw' with wrapper files."
  exit 1
fi

log "Using Maven command: $MVN_CMD"
if [[ "$SKIP_MVN" == "false" ]]; then
  # Best-effort show Maven version
  ("$MVN_CMD" -v | head -n1 || true) | sed 's/^/[build-images-host] /'
fi

# Helper: normalize app.jar for a module
normalize_jar() {
  local module_dir="$1"
  local jar_dir="$module_dir/target"
  [[ -d "$jar_dir" ]] || { err "Missing target directory: $jar_dir"; return 1; }

  # Prefer <artifact>-$VERSION.jar if present, else first non -sources/-javadoc jar
  local cand
  cand=$(ls -1 "$jar_dir"/*-"$VERSION".jar 2>/dev/null | head -n1 || true)
  if [[ -z "$cand" ]]; then
    cand=$(ls -1 "$jar_dir"/*.jar 2>/dev/null | grep -Ev '(-sources|-javadoc)\.jar$' | head -n1 || true)
  fi
  [[ -n "$cand" ]] || { err "No JAR found in $jar_dir. Did the build succeed?"; return 1; }

  cp -f "$cand" "$jar_dir/app.jar"
}

# Step 1: Build once on host (unless skipped)
if [[ "$SKIP_MVN" == "false" ]]; then
  log "Building all modules on host (no Docker) ..."
  (
    cd "$REPO_ROOT"
    "$MVN_CMD" -B -DskipTests -Drevision="$VERSION" -Dchangelist= clean install | sed 's/^/[mvn] /'
  ) || { err "Host Maven build failed"; exit 1; }
  log "✓ Host Maven build complete"
else
  log "Skipping Maven build as requested (--skip-mvn)"
fi

# Step 2: Normalize each module JAR to target/app.jar and build images
build_image() {
  local entry="$1"
  local module="${entry%%:*}"
  local baseImage="${entry#*:}"
  local module_dir="$REPO_ROOT/$module"
  local fullImage
  fullImage="$(image_full_name "$baseImage" "$VERSION")"

  [[ -f "$module_dir/Dockerfile.host" ]] || { err "Missing $module/Dockerfile.host"; return 1; }

  normalize_jar "$module_dir" || return 1

  log "Building image (host JAR): $module -> $fullImage"
  (
    cd "$module_dir"
    docker build -f Dockerfile.host -t "$fullImage" . | sed "s/^/[$module] /"
  ) || { err "Docker build failed for $module"; return 1; }

  if [[ "$PUSH" == "true" ]]; then
    log "Pushing $fullImage"
    docker push "$fullImage" || { err "Push failed for $fullImage"; return 1; }
  fi

  log "✓ Completed: $fullImage"
}

log "Building Docker images from host-built JARs..."
if [[ "$PARALLEL" == "true" ]]; then
  pids=()
  for entry in "${DOCKER_MODULES[@]}"; do
    while [[ ${#pids[@]} -ge $MAX_JOBS ]]; do
      for i in "${!pids[@]}"; do
        ! kill -0 "${pids[$i]}" 2>/dev/null && wait "${pids[$i]}" && unset 'pids[i]'
      done
      pids=("${pids[@]}")
      sleep 0.3
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
