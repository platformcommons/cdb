# /sb-migrate — Generate Flyway Migration

Generate a safe, zero-downtime Flyway migration SQL script for a CDB microservice.

## Usage
```
/sb-migrate <description> [--module <moduleName>] [--operation add-table|add-column|add-index|alter-column|seed]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `description` — what the migration does (e.g., "add provider type table", "add index on api name")
- `--module` — target CDB module service name (e.g., `provider-registry`, `master-data-engine`)
- `--operation` — type of change (helps apply the right safety rules)

### Step 1: Determine next version
Read the DDL directory for the target module:
`cdb-deployment/src/main/resources/database/<module>/ddl/`

Find the highest version number (e.g., `v1.0.2.sql`) and increment the patch for additive changes, or minor version for new tables.

### Step 2: Apply safety rules based on operation

#### add-table (always safe)
```sql
-- v<next>.sql
-- Description: <description>
-- Safe: CREATE TABLE with online DDL

CREATE TABLE <table_name> (
    id           CHAR(36)     NOT NULL DEFAULT (UUID()),
    <columns...>
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    CONSTRAINT pk_<table>   PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_<table>_active     ON <table_name> (active);
CREATE INDEX idx_<table>_created_at ON <table_name> (created_at DESC);
```

#### add-column (safe when nullable or has DEFAULT)
```sql
-- v<next>.sql
-- SAFE: Adding nullable column or column with DEFAULT (MySQL online DDL)
-- NOTE: If adding NOT NULL without DEFAULT — requires backfill migration first

ALTER TABLE <table_name>
    ADD COLUMN <col_name> VARCHAR(255) NULL COMMENT 'Added for <description>';

-- If adding NOT NULL, do it in 3 steps:
-- Step 1 (this migration): ADD COLUMN nullable
ALTER TABLE <table_name> ADD COLUMN <col_name> VARCHAR(255) NULL;
-- Step 2 (separate migration after backfill): UPDATE <table_name> SET <col_name> = ...
-- Step 3 (another migration): ALTER TABLE <table_name> MODIFY <col_name> VARCHAR(255) NOT NULL
```

#### add-index (always safe — MySQL online DDL)
```sql
-- v<next>.sql
-- SAFE: ADD INDEX uses online DDL in MySQL 8 (ALGORITHM=INPLACE, LOCK=NONE)

CREATE INDEX idx_<table>_<col>
    ON <table_name> (<column_list>)
    ALGORITHM=INPLACE LOCK=NONE;

-- Covering index (include all SELECT columns, avoids table row lookup)
CREATE INDEX idx_<table>_<col>_covering
    ON <table_name> (<filter_col>, <sort_col>)
    -- MySQL does not support INCLUDE clause — use composite index instead
    ;

-- Full-text index (if search needed)
ALTER TABLE <table_name>
    ADD FULLTEXT INDEX ft_<table>_<col> (<column>)
    ALGORITHM=INPLACE;
```

#### alter-column (CAUTION — may lock table)
```sql
-- v<next>.sql
-- WARNING: Column type change locks table in MySQL for large tables
-- Safer alternative: add new column → backfill → rename → drop old

-- Option 1 (small table < 100k rows): direct alter
ALTER TABLE <table_name>
    MODIFY COLUMN <col_name> <new_type> NOT NULL
    ALGORITHM=INPLACE LOCK=NONE;  -- add ALGORITHM/LOCK to detect if online possible

-- Option 2 (large table — 3-migration rename pattern):
-- Migration 1 (this migration):
ALTER TABLE <table_name> ADD COLUMN <new_col_name> <new_type> NULL;
-- Migration 2 (after app deploy that writes to both columns):
UPDATE <table_name> SET <new_col_name> = <old_col_name> WHERE <new_col_name> IS NULL;
-- Migration 3 (after verifying backfill complete):
ALTER TABLE <table_name>
    DROP COLUMN <old_col_name>,
    MODIFY COLUMN <new_col_name> <new_type> NOT NULL;
```

#### seed (reference/initial data)
```sql
-- Check for DML directory: cdb-deployment/src/main/resources/database/<module>/dml/
-- v<next>.sql (or in dml/ if it's initial seed data)

INSERT INTO <table_name> (id, name, active, created_at, updated_at, created_by)
VALUES
    (UUID(), '<value1>', TRUE, NOW(), NOW(), 'system'),
    (UUID(), '<value2>', TRUE, NOW(), NOW(), 'system')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    updated_at = NOW();
```

### Step 3: Add Flyway config if missing
Check `application.yml` for:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    out-of-order: false
    validate-on-migrate: true
```

Also verify `pom.xml` has:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### Step 4: Report
- Migration file path created
- Version number used
- Safety assessment: online DDL (no downtime) vs. table lock (requires maintenance window)
- Any multi-step migration pattern needed with deployment instructions
- Indexes added and their justification
