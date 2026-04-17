# /sb-api — Generate Spring Boot REST API

Generate a complete Spring Boot REST API layer (Controller + Service + DTOs + Exceptions) for a CDB microservice.

## Usage
```
/sb-api <ResourceName> [--module <moduleName>] [--base-path <apiPath>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `ResourceName` — PascalCase name of the resource (e.g., `Provider`, `ApiDefinition`)
- `--module` — target module name (e.g., `provider-registry`); if omitted, infer from context
- `--base-path` — REST base path (default: `/api/v1/<kebab-case-name>s`)

### Step 1: Discover the module
1. Find the target service directory (`cdb-<module>/`)
2. Read `src/main/java/.../` to find existing package structure and entity if present
3. Read one existing controller to match exact patterns already used

### Step 2: Generate DTOs (Java records)
```java
// <ResourceName>Dto.java — response DTO
public record <ResourceName>Dto(
    UUID id,
    String name,           // replace with actual fields
    boolean active,
    LocalDateTime createdAt,
    String createdBy
) {}

// Create<ResourceName>Request.java
public record Create<ResourceName>Request(
    @NotBlank @Size(max = 255) String name
    // add fields without id/audit
) {}

// Update<ResourceName>Request.java
public record Update<ResourceName>Request(
    @NotBlank @Size(max = 255) String name,
    boolean active
) {}
```

### Step 3: Generate domain exceptions
```java
// <ResourceName>NotFoundException.java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class <ResourceName>NotFoundException extends RuntimeException {
    public <ResourceName>NotFoundException(UUID id) {
        super("<ResourceName> not found: " + id);
    }
}

// <ResourceName>DuplicateException.java
@ResponseStatus(HttpStatus.CONFLICT)
public class <ResourceName>DuplicateException extends RuntimeException {
    public <ResourceName>DuplicateException(String name) {
        super("<ResourceName> already exists: " + name);
    }
}
```

### Step 4: Generate service
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class <ResourceName>Service {

    private final <ResourceName>Repository <resourceName>Repository;
    private final <ResourceName>Mapper <resourceName>Mapper;

    public Page<<ResourceName>Dto> findAll(Pageable pageable) {
        return <resourceName>Repository.findByActiveTrue(pageable)
            .map(<resourceName>Mapper::toDto);
    }

    public <ResourceName>Dto findById(UUID id) {
        return <resourceName>Repository.findById(id)
            .filter(<ResourceName>::isActive)
            .map(<resourceName>Mapper::toDto)
            .orElseThrow(() -> new <ResourceName>NotFoundException(id));
    }

    @Transactional
    public <ResourceName>Dto create(Create<ResourceName>Request request) {
        if (<resourceName>Repository.existsByNameIgnoreCase(request.name())) {
            throw new <ResourceName>DuplicateException(request.name());
        }
        return <resourceName>Mapper.toDto(
            <resourceName>Repository.save(<resourceName>Mapper.toEntity(request))
        );
    }

    @Transactional
    public <ResourceName>Dto update(UUID id, Update<ResourceName>Request request) {
        <ResourceName> entity = <resourceName>Repository.findById(id)
            .filter(<ResourceName>::isActive)
            .orElseThrow(() -> new <ResourceName>NotFoundException(id));
        <resourceName>Mapper.updateEntity(request, entity);
        return <resourceName>Mapper.toDto(entity);
    }

    @Transactional
    public void delete(UUID id) {
        <ResourceName> entity = <resourceName>Repository.findById(id)
            .orElseThrow(() -> new <ResourceName>NotFoundException(id));
        entity.setActive(false);
    }
}
```

### Step 5: Generate controller
```java
@RestController
@RequestMapping("<apiBasePath>")
@RequiredArgsConstructor
@Tag(name = "<ResourceName>s", description = "<ResourceName> management")
public class <ResourceName>Controller {

    private final <ResourceName>Service <resourceName>Service;

    @GetMapping
    @Operation(summary = "List all <resourceName>s")
    public ResponseEntity<Page<<ResourceName>Dto>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(<resourceName>Service.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get <resourceName> by ID")
    public ResponseEntity<<ResourceName>Dto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(<resourceName>Service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new <resourceName>")
    public ResponseEntity<<ResourceName>Dto> create(
            @Valid @RequestBody Create<ResourceName>Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(<resourceName>Service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update <resourceName>")
    public ResponseEntity<<ResourceName>Dto> update(
            @PathVariable UUID id,
            @Valid @RequestBody Update<ResourceName>Request request) {
        return ResponseEntity.ok(<resourceName>Service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete <resourceName>")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        <resourceName>Service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 6: Verify global exception handler
Check if a `@RestControllerAdvice` class exists. If not, create one:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", message));
    }

    record ErrorResponse(String code, String message) {}
}
```

### Step 7: Report
List all files created, the exact API contract (endpoints + HTTP methods), and tell the user to run `/sb-entity <ResourceName>` if the entity doesn't exist yet.
