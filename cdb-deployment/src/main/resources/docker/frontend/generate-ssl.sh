#!/bin/bash

# Generate self-signed SSL certificates for development
set -e

echo "Generating SSL certificates for CDB domains..."

mkdir -p ssl

echo "Generating certificate for *.platformcommons.org..."
openssl genrsa -out ssl/key.pem 2048
openssl req -new -key ssl/key.pem -out ssl/cert.csr -subj "/C=US/ST=State/L=City/O=CDB Platform/CN=*.platformcommons.org"
openssl x509 -req -days 365 -in ssl/cert.csr -signkey ssl/key.pem -out ssl/cert.pem
rm ssl/cert.csr

echo "SSL certificates generated in ./ssl/"
echo "For production, replace with proper certificates from a CA."