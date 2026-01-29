#!/bin/bash

# Build script for React UIs
set -e

echo "Building CDB Frontend UIs..."

# Build API Registry UI
echo "Building API Registry UI..."
cd ../../../../../../cdb-api-registry/ui
npm ci
npm run build
cp -r dist/* ../../cdb-deployment/src/main/resources/docker/frontend/api-registry-ui/

# Build Provider Registry UI  
echo "Building Provider Registry UI..."
cd ../../cdb-provider-registry/ui
npm ci
npm run build
cp -r dist/* ../../cdb-deployment/src/main/resources/docker/frontend/provider-registry-ui/

echo "UI builds completed successfully!"
echo "Run 'docker-compose up -d' to start the frontend services."