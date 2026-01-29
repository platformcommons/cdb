# CDB Frontend Deployment

Docker setup for deploying React UIs with Nginx, SSL, and domain routing.

## Domain Configuration
- `cdb.platformcommons.org` → API Registry UI
- `cdb.platformcommons.org/provider-registry` → Provider Registry UI

## Quick Start

1. **Generate SSL certificates:**
   ```bash
   ./generate-ssl.sh
   ```

2. **Build React UIs:**
   ```bash
   ./build-ui.sh
   ```

3. **Start services:**
   ```bash
   docker compose up -d
   ```

## Directory Structure
```
frontend/
├── api-registry-ui/          # Built API Registry React app
├── provider-registry-ui/     # Built Provider Registry React app
├── ssl/                      # SSL certificates
├── nginx.conf               # Nginx configuration
├── docker-compose.yml       # Service orchestration
├── Dockerfile.nginx         # Nginx container
├── build-ui.sh             # UI build script
└── generate-ssl.sh         # SSL certificate generator
```

## Production Notes
- Replace self-signed certificates with CA-issued certificates
- Update DNS to point `cdb.platformcommons.org` to your server
- Consider using Let's Encrypt for automatic SSL renewal
- Adjust security headers and CORS policies as needed