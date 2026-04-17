# /sb-security — Configure Spring Security for a CDB Service

Audit and configure Spring Security for a CDB microservice: public/protected endpoint rules, method-level security, and security context usage.

## Usage
```
/sb-security [--module <moduleName>] [--audit] [--add-public <path>] [--add-role <role:path>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `--module` — target CDB module
- `--audit` — if present, audit the existing security config and report gaps
- `--add-public` — add a path to the public whitelist (no JWT required)
- `--add-role` — add role-based access control for a path (e.g., `ADMIN:/api/v1/admin/**`)

### Step 1: Read existing security config
1. Read `cdb-shared-libraries/cdb-security-lib/src/main/java/.../config/BaseSecurityConfig.java`
2. Find any existing security config class in the target module that extends `BaseSecurityConfig`
3. Read `application.yml` for the target module to check `cdb.security` properties

### Step 2: Audit mode (when --audit)
Check for these security gaps:

- [ ] **Missing JWT protection** — endpoints not covered by the filter chain
- [ ] **Hardcoded credentials** — any literal passwords or secrets
- [ ] **Missing CSRF config** — for REST APIs, CSRF should be disabled
- [ ] **Missing CORS config** — or overly permissive `allowedOrigins("*")`
- [ ] **Controllers returning entities** — DTOs must be used, never JPA entities
- [ ] **Missing input validation** — `@Valid` on all `@RequestBody` params
- [ ] **SQL injection risk** — native queries with string concatenation
- [ ] **Missing method security** — admin endpoints not protected with `@PreAuthorize`
- [ ] **Actuator endpoints exposed** — `/actuator/**` should be restricted

### Step 3: Generate or update security config

```java
package com.platformcommons.cdb.<module>.config;

import com.platformcommons.cdb.security.config.BaseSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends BaseSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return super.configure(http)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no JWT needed
                .requestMatchers(
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                    // add --add-public paths here
                ).permitAll()
                // Role-based endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/etl/**").hasAnyRole("ADMIN", "ETL_OPERATOR")
                // All other API endpoints require valid JWT
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .build();
    }

    @Override
    protected String[] publicEndpoints() {
        return new String[]{
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**"
        };
    }
}
```

### Step 4: Add method-level security where needed

For admin-only operations, add `@PreAuthorize` to service methods:
```java
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public void deleteAll() { ... }

// Access JWT claims in service:
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
public UserDto findById(String userId) { ... }
```

### Step 5: Add CORS config for frontend
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of(
        "${cdb.security.cors.allowed-origins:http://localhost:4200}"
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Add to `application.yml`:
```yaml
cdb:
  security:
    cors:
      allowed-origins: ${CDB_CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

### Step 6: Actuator security
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: ${CDB_ACTUATOR_HEALTH_DETAIL:never}
  server:
    port: ${CDB_MANAGEMENT_PORT:9090}  # separate port from API for network-level isolation
```

### Step 7: Report
- Current security posture assessment
- Files modified (security config, application.yml)
- Public endpoints list (with justification for each)
- Role-protected endpoints list
- Any vulnerabilities found (with specific file:line references)
- Remaining recommendations that require manual review
