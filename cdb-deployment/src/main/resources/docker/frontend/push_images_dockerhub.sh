#!/bin/bash

# Usage: ./push_image_dockerhub.sh <version>
# Description:
#   Pushes locally built images to Docker Hub public repositories under the
#   'platformcommons' namespace. It will:
#     - docker login -u platformcommons
#     - retag local images (built as cdb/<name>:<version>) to platformcommons/<name>:<version>
#     - push them to Docker Hub
#
# Example:
#   ./push_image_dockerhub.sh 1.0.0
#

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Error: Please provide a version tag."
  echo "Usage: $0 <version>"
  exit 1
fi

VERSION=$1
VERSION_IMG=latest
# Local images expected to be present as cdb/<repo>:<version>
IMAGES=(
  "frontend-cdb-frontend"
)

# Login to Docker Hub using the provided username
echo "Logging into Docker Hub as 'platformcommons'..."
docker login -u platformcommons || { echo "Docker login failed"; exit 1; }

# Tag and push each image to Docker Hub under platformcommons namespace
for IMAGE in "${IMAGES[@]}"; do
  IMAGE_ID=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "^$IMAGE:$VERSION_IMG$" || true)
  if [ -z "$IMAGE_ID" ]; then
    echo "Image $IMAGE:$VERSION_IMG not found locally. Skipping..."
    continue
  fi

  # Push to Docker Hub under the same repository name within the 'platformcommons' namespace
  TARGET_IMAGE="platformcommons/cdb-frontend:$VERSION"
  
  echo "Tagging $IMAGE:$VERSION as $TARGET_IMAGE"
  docker tag "$IMAGE:$VERSION_IMG" "$TARGET_IMAGE"

  echo "Pushing $TARGET_IMAGE..."
  docker push "$TARGET_IMAGE" || { echo "Push failed for $TARGET_IMAGE"; exit 1; }

done

echo "All done pushing to Docker Hub!"
