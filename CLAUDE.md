# CDB Platform — Claude Code Project Context

## Project Overview

CDB (Common Digital Backbone) is an enterprise Spring Boot microservices platform with an Angular frontend and MySQL persistence. It follows a multi-module Maven structure with shared libraries consumed by independent services.

## Repository Layout

```
cdb/
├── cdb-shared-libraries/
│   ├── cdb-common-core/          # BaseEntity, JPA audit, Jackson config, OpenAPI base
│   └── cdb-security-lib/         # RS256 JWT, AuthFilter, BaseSecurityConfig
├── cdb-provider-registry/        # Provider management service (port 8081) + Angular UI
├── cdb-api-registry/             # API catalogue service (port 8082) + Angular UI
├── cdb-auth-registry/            # Auth/JWT issuer service (port 8083)
├── cdb-api-gateway/              # Spring Cloud Gateway (port 8080, reactive)
├── cdb-master-data-engine/       # Master data CRUD service (port 8084) + Angular UI
└── cdb-deployment/               # Docker Compose, Dockerfiles, DB DDL/DML scripts
```

## Technology Stack

### Backend
- **Java 21**, Spring Boot 3.x, Spring Cloud Gateway (reactive)
- **MySQL 8** with Flyway migrations (DDL in `cdb-deployment/src/main/resources/database/`)
- **JPA/Hibernate** with `BaseEntity` (UUID PK, created/updated audit)
- **Security**: RS256 JWT via `cdb-security-lib`; private key only in auth-registry
- **HikariCP** connection pool, Caffeine/Redis for caching
- **Lombok**, MapStruct, springdoc-openapi

### Frontend
- **Angular 17+** with standalone components, Signals, NgRx where needed
- Per-service UI embedded in `<service>/ui/` and served via `UiForwardController`
- Reactive Forms, Angular Material / PrimeNG

### ETL / Reporting
- **Spring Batch** for scheduled/triggered batch jobs
- **Apache POI** (Excel), JasperReports for report generation
- Direct MySQL queries via JdbcTemplate for read-heavy reporting

## Three Verticals

### 1. Angular Vertical
Responsible for all frontend development: components, services, state management, routing, forms, testing.

Key agents: `angular-architect`, `angular-developer`, `angular-tester`
Key commands: `/ng-component`, `/ng-service`, `/ng-state`, `/ng-optimize`, `/ng-test`

### 2. Spring Boot + MySQL Vertical
Responsible for backend REST APIs, JPA entities, services, repositories, security, migrations, performance tuning.

Key agents: `spring-architect`, `spring-developer`, `spring-dba`
Key commands: `/sb-api`, `/sb-entity`, `/sb-cache`, `/sb-migrate`, `/sb-security`

### 3. ETL & Reporting Vertical
Responsible for data pipelines, batch processing, transformations, and report generation.

Key agents: `etl-architect`, `etl-engineer`, `report-engineer`
Key commands: `/etl-pipeline`, `/etl-transform`, `/etl-report`

## Coding Conventions

### Backend
- Package root: `com.platformcommons.cdb.<module>`
- All entities extend `BaseEntity` (provides `id` UUID, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`)
- DTOs are plain records or Lombok `@Data` classes; never expose entities directly
- Repository interfaces extend `JpaRepository`; use `@Query` for complex JPQL
- Service classes annotated `@Service @Transactional(readOnly = true)` with write methods overriding to `@Transactional`
- Controller classes annotated `@RestController @RequestMapping("/api/v1/...")`; use `@Tag` for OpenAPI grouping
- Environment-driven config only — no hardcoded values; use `${ENV_VAR:default}` pattern

### Frontend (Angular)
- Standalone components only (no NgModules except app root)
- Use `inject()` over constructor injection
- Signals (`signal`, `computed`, `effect`) for local state; NgRx Signals Store for shared state
- `OnPush` change detection on all components
- Reactive Forms with typed form groups
- HTTP calls in services only, never in components

### ETL / Batch
- Spring Batch `Job` → `Step` → `ItemReader` / `ItemProcessor` / `ItemWriter`
- JdbcPagingItemReader for large dataset extraction
- Chunk size default: 500; adjust per dataset volume
- Always define `JobParameters` for idempotent re-runs

## Build Commands

```bash
# Build all
mvn -DskipTests clean package

# Build single module
mvn -pl cdb-provider-registry -am -DskipTests clean package

# Build container image
mvn -pl cdb-provider-registry -am spring-boot:build-image

# Angular (inside ui/ directory)
npm install && ng build --configuration=production

# Run platform
docker compose -f cdb-deployment/src/main/resources/docker/backend/docker-compose.yml up -d
```

## Multi-Agent Orchestration

The `orchestrator` agent is the entry point for any task that spans multiple verticals or is ambiguous. It decomposes the task and spawns the appropriate vertical agents in parallel where possible, then synthesizes results.

Orchestration pattern:
1. Analyze request → identify which verticals are involved
2. Spawn vertical agents in parallel for independent work
3. Coordinate sequential handoffs (e.g., Spring Boot API first, then Angular service)
4. Validate cross-cutting concerns (security, error handling, API contracts)
