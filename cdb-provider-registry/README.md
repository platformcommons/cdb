# CDB Provider Registry

Spring Boot service for registering and discovering providers within the CDB ecosystem. See architecture guide for controllers, services, repositories, models, config, and DTOs. OpenAPI and Security configuration templates apply here.

## Embedded React UI
This service now embeds a lightweight React single‑page application (SPA) that provides:
- Product landing page
- Login page
- Signup page
- Top navigation menu

The UI code lives under `ui/` and is built with Vite. During Maven build, the UI is compiled and copied into `classpath:/static`, so it is served by Spring Boot at runtime.

### Build and run
- Build everything (backend + UI):
  - `mvn -pl cdb-provider-registry -am clean package`
- Run locally:
  - `mvn -pl cdb-provider-registry spring-boot:run`
- Access the app in browser:
  - `http://localhost:8080/`

### Develop UI with hot reload
From `cdb-provider-registry/ui`:
- `npm ci`
- `npm run dev`

Optional: configure the dev server to proxy API calls to Spring Boot if you add APIs later.

### Decoupling for scale (deploy UI independently)
This module is designed so that the UI can be carved out later without backend changes:
- Skip building and packaging the UI using a Maven profile:
  - `mvn -pl cdb-provider-registry -Pskip-ui clean package`
- When skipped, you can host the React UI separately (e.g., S3/CloudFront, Nginx). Point it to the provider-registry APIs via environment variables and CORS.
- The frontend-maven-plugin and `ui/` folder can be moved to a new repository with minimal effort when you decide to split.

### Notes
- SPA routing is supported by forwarding `/, /login, /signup, /app/**` to `index.html` in `UiForwardController`.
- Spring Security is configured to permit static assets and SPA routes. Harden further as APIs are implemented.