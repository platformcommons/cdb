# /etl-transform — Generate Data Transformation Logic

Generate a Spring Batch ItemProcessor with validation, enrichment, data type conversions, and business rule application for a CDB ETL pipeline.

## Usage
```
/etl-transform <ProcessorName> [--input <InputType>] [--output <OutputType>] [--rules "<rule1,rule2,...>"]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `ProcessorName` — PascalCase name (e.g., `ProviderEnrichment`, generates `ProviderEnrichmentProcessor`)
- `--input` — input DTO type (raw extracted record)
- `--output` — output DTO type (processed record ready for writing)
- `--rules` — comma-separated transformation rules (e.g., `validate-status,normalize-name,enrich-category,compute-score`)

### Step 1: Read input/output DTO types
Read any existing DTO files mentioned in `--input` / `--output` to understand field structures.

### Step 2: Generate composite processor with clear rule separation

```java
@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class <ProcessorName>Processor implements ItemProcessor<<InputType>, <OutputType>> {

    // Inject repositories/services needed for enrichment
    private final CategoryRepository categoryRepository;
    private final ValidationRuleEngine validationEngine;

    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong skippedCount = new AtomicLong(0);

    @Override
    public <OutputType> process(<InputType> input) {
        try {
            // 1. Validate — return null to skip invalid records
            ValidationResult validation = validate(input);
            if (!validation.isValid()) {
                log.warn("Skipping record {} — {}", input.id(), validation.reason());
                skippedCount.incrementAndGet();
                return null;
            }

            // 2. Normalize
            <InputType> normalized = normalize(input);

            // 3. Enrich
            EnrichmentContext context = enrich(normalized);

            // 4. Transform to output
            <OutputType> output = transform(normalized, context);

            processedCount.incrementAndGet();
            return output;

        } catch (Exception e) {
            log.error("Error processing record {}: {}", input.id(), e.getMessage());
            throw e; // re-throw so Spring Batch skip/retry policies handle it
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private ValidationResult validate(<InputType> input) {
        if (input.id() == null || input.id().isBlank()) {
            return ValidationResult.invalid("Missing required field: id");
        }
        if (input.status() == null) {
            return ValidationResult.invalid("Missing required field: status");
        }
        try {
            ProviderStatus.valueOf(input.status()); // validate enum value
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid("Invalid status: " + input.status());
        }
        return ValidationResult.valid();
    }

    // ─── Normalization ────────────────────────────────────────────────────────

    private <InputType> normalize(<InputType> input) {
        return input.withName(
            input.name() == null ? "UNKNOWN" :
            input.name().trim().toUpperCase()
        ).withStatus(
            input.status().trim().toUpperCase()
        );
    }

    // ─── Enrichment ───────────────────────────────────────────────────────────

    private EnrichmentContext enrich(<InputType> input) {
        String categoryName = categoryRepository.findNameById(input.categoryId())
            .orElse("UNCATEGORIZED");

        return new EnrichmentContext(categoryName);
    }

    // ─── Transform ────────────────────────────────────────────────────────────

    private <OutputType> transform(<InputType> input, EnrichmentContext ctx) {
        return new <OutputType>(
            UUID.randomUUID().toString(),
            input.id(),
            input.name(),
            input.status(),
            ctx.categoryName(),
            computeScore(input),
            LocalDate.now()
        );
    }

    // ─── Business Rules ───────────────────────────────────────────────────────

    private int computeScore(<InputType> input) {
        int score = 50; // base score
        if ("ACTIVE".equals(input.status())) score += 30;
        if (input.name() != null && input.name().length() > 3) score += 20;
        return Math.min(score, 100);
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("[<ProcessorName>] Processed: {}, Skipped: {}",
            processedCount.get(), skippedCount.get());
        stepExecution.getExecutionContext().putLong("processedCount", processedCount.get());
        stepExecution.getExecutionContext().putLong("skippedCount", skippedCount.get());
        return stepExecution.getExitStatus();
    }
}
```

### Step 3: Generate ValidationResult helper
```java
public record ValidationResult(boolean valid, String reason) {
    public boolean isValid() { return valid; }
    public static ValidationResult valid() { return new ValidationResult(true, null); }
    public static ValidationResult invalid(String reason) { return new ValidationResult(false, reason); }
}
```

### Step 4: Generate EnrichmentContext
```java
public record EnrichmentContext(
    String categoryName
    // add enrichment fields
) {}
```

### Step 5: Add skip policy for validation failures
In the Step configuration, ensure:
```java
.faultTolerant()
    .skip(ValidationException.class)        // skip bad data silently (logged above)
    .skip(EmptyResultDataAccessException.class) // skip missing lookups
    .skipLimit(Integer.MAX_VALUE)           // for validation — skip all bad records
    .noRollback(ValidationException.class)  // validation failures don't need rollback
    .retry(TransientDataAccessException.class) // DB timeouts — retry
    .retryLimit(3)
```

### Step 6: Report
- Processor class created with all transformation rules applied
- Validation rules implemented and which fields they check
- Enrichment lookups and their fallback values
- Skip policies configured and justification
- How to plug this processor into an existing pipeline from `/etl-pipeline`
