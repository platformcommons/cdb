# /sb-entity — Generate JPA Entity + Repository + MapStruct Mapper

Generate an optimized JPA entity extending BaseEntity, with repository, MapStruct mapper, and Flyway migration for a CDB microservice.

## Usage
```
/sb-entity <EntityName> [--module <moduleName>] [--fields "<field:type,...>"] [--relations "<relation>"]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `EntityName` — PascalCase entity name
- `--module` — target CDB module (e.g., `master-data-engine`)
- `--fields` — comma-separated `fieldName:JavaType` pairs (e.g., `name:String,active:boolean,code:String`)
- `--relations` — e.g., `ManyToOne:Category`, `OneToMany:Tag`

### Step 1: Read existing patterns
1. Read `cdb-shared-libraries/cdb-common-core/src/main/java/.../jpa/BaseEntity.java` to understand the base class
2. Find one existing entity in the target module to match table naming, index conventions, and annotation style

### Step 2: Generate entity
```java
package com.platformcommons.cdb.<module>.domain;

import com.platformcommons.cdb.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "<snake_case_name>",
    indexes = {
        @Index(name = "idx_<table>_active", columnList = "active"),
        @Index(name = "idx_<table>_name", columnList = "name")
        // add composite indexes for known query patterns
    }
)
@Getter @Setter @NoArgsConstructor
public class <EntityName> extends BaseEntity {

    // Add each field from --fields, choosing:
    // String  → @Column(name="...", nullable=false, length=255)
    // TEXT    → @Column(name="...", columnDefinition="TEXT")
    // boolean → @Column(name="...", nullable=false) with = true default
    // UUID FK → @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="..._id")
    // LocalDateTime → @Column(name="...", nullable=false)

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // If --relations includes ManyToOne:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // If --relations includes OneToMany:
    @OneToMany(mappedBy = "<entityName>", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();
}
```

**Rules:**
- Always `FetchType.LAZY` on all associations
- Never `CascadeType.REMOVE` alone — use `orphanRemoval = true` on parent side
- Index every foreign key column
- Index every column used in WHERE clauses
- Use `@Column(nullable = false)` — never rely on DB defaults alone

### Step 3: Generate repository
```java
package com.platformcommons.cdb.<module>.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface <EntityName>Repository extends JpaRepository<<EntityName>, UUID> {

    Page<<EntityName>> findByActiveTrue(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    // Example JOIN FETCH to avoid N+1 on category:
    @Query("SELECT e FROM <EntityName> e JOIN FETCH e.category WHERE e.active = true AND e.id = :id")
    Optional<<EntityName>> findByIdWithCategory(UUID id);
}
```

### Step 4: Generate MapStruct mapper
```java
package com.platformcommons.cdb.<module>.mapper;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface <EntityName>Mapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    <EntityName>Dto toDto(<EntityName> entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "category", ignore = true)
    <EntityName> toEntity(Create<EntityName>Request request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntity(Update<EntityName>Request request, @MappingTarget <EntityName> entity);
}
```

### Step 5: Generate Flyway migration
Determine next version by reading existing migration files in `cdb-deployment/src/main/resources/database/<module>/ddl/`.

```sql
-- v<next>.sql
CREATE TABLE <snake_case_name> (
    id           CHAR(36)     NOT NULL DEFAULT (UUID()),
    name         VARCHAR(255) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- add all columns from --fields
    category_id  CHAR(36),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    CONSTRAINT pk_<table>        PRIMARY KEY (id),
    CONSTRAINT uq_<table>_name   UNIQUE (name),
    CONSTRAINT fk_<table>_cat    FOREIGN KEY (category_id)
                                  REFERENCES category (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_<table>_active      ON <snake_case_name> (active);
CREATE INDEX idx_<table>_category_id ON <snake_case_name> (category_id);
CREATE INDEX idx_<table>_created_at  ON <snake_case_name> (created_at DESC);
```

### Step 6: Report
- Files created (entity, repository, mapper, migration)
- Indexes created and their justification
- Any N+1 risks flagged (associations without JOIN FETCH)
- Suggest running `/sb-api <EntityName>` next to generate the REST layer
