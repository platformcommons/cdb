#!/bin/bash

# Usage: ./push-to-nexus.sh 1.0.0

if [ -z "$1" ]; then
  echo "Error: Please provide a version tag."
  echo "Usage: $0 <version>"
  exit 1
fi

VERSION=$1
NEXUS_URL="nexus.platformcommons.org"
IMAGES=(
  "cdb/cdb-api-gateway"
  "cdb/cdb-auth-registry"
  "cdb/cdb-provider-registry"
  "cdb/cdb-api-registry"
)

# Login to Nexus
echo "Logging into Nexus..."
docker login $NEXUS_URL || { echo "Docker login failed"; exit 1; }

# Tag and push each image
for IMAGE in "${IMAGES[@]}"; do
  IMAGE_ID=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "^$IMAGE:$VERSION$")
  if [ -z "$IMAGE_ID" ]; then
    echo "Image $IMAGE:$VERSION not found locally. Skipping..."
    continue
  fi

  TARGET_IMAGE="$NEXUS_URL/$IMAGE:$VERSION"
  echo "Tagging $IMAGE:$VERSION as $TARGET_IMAGE"
  docker tag "$IMAGE:$VERSION" "$TARGET_IMAGE"

  echo "Pushing $TARGET_IMAGE..."
  docker push "$TARGET_IMAGE" || { echo "Push failed for $TARGET_IMAGE"; exit 1; }
done

echo "All done!"
