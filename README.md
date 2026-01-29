# CDB Platform - Multi-Module Maven Project

This repository contains a multi-module Maven setup for the CDB (Common Digital Backbone) platform. It provides an enterprise-grade architecture with clear separation between shared libraries and Spring Boot microservices.

## Modules

- cdb-shared-libraries
  - cdb-common-core
  - cdb-security-lib
  - cdb-registry-client
  - cdb-monitoring-lib
- cdb-provider-registry
- cdb-api-registry
- cdb-auth-registry
- cdb-api-gateway
- cdb-master-data-engine

## Build

Build all modules and create runnable JARs:
- mvn -DskipTests clean package

Build container images for each component (uses Spring Boot build-image):
- mvn -pl cdb-provider-registry -am spring-boot:build-image
- mvn -pl cdb-api-registry -am spring-boot:build-image
- mvn -pl cdb-auth-registry -am spring-boot:build-image
- mvn -pl cdb-api-gateway -am spring-boot:build-image
- mvn -pl cdb-master-data-engine -am spring-boot:build-image

Tip: If your local JDK is not 21, use the helper script which ensures Maven runs with JDK 21 via Docker automatically:
- cd cdb-deployment/src/main/resources/docker/backend && ./build-images.sh 1.0.0

Script runtime JDK selection
- The script always uses Dockerized Maven with JDK 21, independent of the host JDK/Maven.
- Example with push and registry prefix:
  REGISTRY=ghcr.io/my-org ./build-images.sh 1.0.0 --push

Images will be tagged as:
- cdb/cdb-provider-registry:1.0.0
- cdb/cdb-api-registry:1.0.0
- cdb/cdb-auth-registry:1.0.0
- cdb/cdb-api-gateway:1.0.0
- cdb/cdb-master-data-engine:1.0.0

## Run the platform (Docker Compose)

- docker compose up -d

Services and ports:
- API Gateway: http://localhost:8080
- Provider Registry: http://localhost:8081
- API Registry: http://localhost:8082
- Auth Registry: http://localhost:8083
- Master Data Engine: http://localhost:8084

### Database connection from containers
- If your MySQL database runs on the host machine, containers cannot use `localhost:3306` to reach it.
- Use `host.docker.internal` as the DB host. This repo’s backend compose file maps it to the Docker host via `host-gateway` for Linux/macOS/Windows.
- Default `.env` sets `CDB_DB_HOST=host.docker.internal`. Override as needed (e.g., to a remote DB host or another service name).

## OpenAPI & Swagger UI
After starting services, Swagger UI is available at:
- Gateway: http://localhost:8080/swagger-ui.html
- Provider Registry: http://localhost:8081/swagger-ui.html
- API Registry: http://localhost:8082/swagger-ui.html
- Auth Registry: http://localhost:8083/swagger-ui.html
- Master Data Engine: http://localhost:8084/swagger-ui.html

The raw OpenAPI JSON is available under /v3/api-docs in each service.

## Java version requirements

This project is configured for Java 21 for both development and deployment.

- Local development: Install JDK 21 and ensure your JAVA_HOME and PATH use it.
  - With SDKMAN: sdk install java 21.0.4-tem && sdk use java 21.0.4-tem
  - With Homebrew (macOS): brew install openjdk@21
  - With apt (Ubuntu): sudo apt-get install -y openjdk-21-jdk
- Maven build: The parent POM sets <java.version>21</java.version>. Build with:
  - mvn -DskipTests clean package
- Container images: Spring Boot buildpacks are configured to use Java 21 (BP_JVM_VERSION=21) in each service POM, so images run on JRE 21.

Optional: Maven Toolchains
- To pin Maven to JDK 21 regardless of your current JAVA_HOME, create ~/.m2/toolchains.xml with:

  <toolchains>
    <toolchain>
      <type>jdk</type>
      <provides>
        <version>21</version>
        <vendor>any</vendor>
      </provides>
      <configuration>
        <jdkHome>/path/to/your/jdk-21</jdkHome>
      </configuration>
    </toolchain>
  </toolchains>

CI/CD
- Ensure your CI runners have JDK 21 installed and configured. For container-based builds, no change is required other than using the provided build-image goals which already target Java 21.

## JWT configuration: RS256 with default PEMs and JWKS

The shared security library (cdb-security-lib) provides JwtTokenService, now RS256-only (asymmetric).

- Default: The library ships with development-only RSA PEMs embedded. These are used if you do not configure keys.
- Production: Override the keys via environment variables or application.yml. Only the Auth service needs the private key; validators need only the public key.

### Property keys
- cdb.security.jwt.rsa.private-key (PEM PKCS#8 private key, required on issuer only; optional elsewhere)
- cdb.security.jwt.rsa.public-key (PEM X.509 public key, required on all services)
- cdb.security.jwt.kid (optional key id; included in JWT header and JWKS)

You can supply properties in any standard Spring way:
- application.yml / application.properties
- Environment variables (e.g., CDB_SECURITY_JWT_RSA_PRIVATE_KEY, CDB_SECURITY_JWT_RSA_PUBLIC_KEY, CDB_SECURITY_JWT_KID)
- Java system properties
- External secret managers (mounted as env or file and referenced in config)

### RS256 configuration examples
PEM format requirements
- Private key: PKCS#8 (-----BEGIN PRIVATE KEY-----)
- Public key: X.509 SubjectPublicKeyInfo (-----BEGIN PUBLIC KEY-----)

Issuer (Auth service) example (application.yml):

  cdb:
    security:
      jwt:
        kid: "key-2025-09"
        rsa:
          private-key: |
            -----BEGIN PRIVATE KEY-----
            ...base64...
            -----END PRIVATE KEY-----
          public-key: |
            -----BEGIN PUBLIC KEY-----
            ...base64...
            -----END PUBLIC KEY-----

Validator services example (no private key):

  cdb:
    security:
      jwt:
        rsa:
          public-key: |
            -----BEGIN PUBLIC KEY-----
            ...base64...
            -----END PUBLIC KEY-----

JWKS endpoint
- The Auth service exposes the JWKS at /.well-known/jwks.json (and /jwks.json).

### Docker Compose (RS256)
To override the embedded development keys, set env variables:

  environment:
    CDB_SECURITY_JWT_RSA_PRIVATE_KEY: |
      -----BEGIN PRIVATE KEY-----
      ...
      -----END PRIVATE KEY-----
    CDB_SECURITY_JWT_RSA_PUBLIC_KEY: |
      -----BEGIN PUBLIC KEY-----
      ...
      -----END PUBLIC KEY-----
    CDB_SECURITY_JWT_KID: key-2025-09

### Security note
- The embedded PEMs are for development only. Replace them in all deployed environments.
- Never use production private keys in source control or third-party tools.
- Keep the private key only in the Auth service or a secure key manager; distribute only the public key to validators.

### DB Query
```
CREATE DATABASE cdb_master_data_engine_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
```aiignore


```