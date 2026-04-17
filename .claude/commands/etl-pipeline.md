# /etl-pipeline — Design and Generate Spring Batch Pipeline

Generate a complete Spring Batch job with reader, processor, writer, partitioning, and REST trigger endpoint for the CDB platform.

## Usage
```
/etl-pipeline <JobName> [--module <moduleName>] [--source <table/query>] [--target <table>] [--chunk <size>] [--schedule <cron>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `JobName` — PascalCase job name (e.g., `ProviderSummary`, generates `providerSummaryJob`)
- `--module` — CDB module to add the job to (default: create in `cdb-master-data-engine`)
- `--source` — source table or SQL hint (e.g., `provider`, `api_definition WHERE active=1`)
- `--target` — destination table for processed output
- `--chunk` — chunk size (default: 500; use 200 for complex processing, 1000 for simple copy)
- `--schedule` — cron expression (e.g., `0 0 2 * * ?` for 2 AM daily); if omitted, event-triggered only

### Step 1: Determine dataset scale
Ask (or infer from context):
- Estimated row count: determines single-threaded vs. partitioned strategy
- Processing complexity: determines chunk size
- Schedule: cron vs. event-triggered vs. both

### Step 2: Generate batch configuration class
```java
@Configuration
@RequiredArgsConstructor
public class <JobName>BatchConfig {

    private final DataSource dataSource;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // ─── Reader ──────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public JdbcPagingItemReader<<JobName>RawDto> <jobName>Reader(
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate) {

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id, name, status, created_at");
        queryProvider.setFromClause("FROM <source_table>");
        queryProvider.setWhereClause("WHERE active = 1 AND created_at BETWEEN :startDate AND :endDate");
        queryProvider.setSortKeys(Map.of("created_at", Order.ASCENDING, "id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<<JobName>RawDto>()
            .name("<jobName>Reader")
            .dataSource(dataSource)
            .queryProvider(queryProvider)
            .parameterValues(Map.of("startDate", startDate, "endDate", endDate))
            .pageSize(<chunk>)
            .rowMapper(new <JobName>RowMapper())
            .build();
    }

    // ─── Processor ────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public <JobName>Processor <jobName>Processor() {
        return new <JobName>Processor();
    }

    // ─── Writer ───────────────────────────────────────────────────────────────

    @Bean
    public JdbcBatchItemWriter<<JobName>OutputDto> <jobName>Writer() {
        return new JdbcBatchItemWriterBuilder<<JobName>OutputDto>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO <target_table> (id, source_id, processed_value, report_date)
                VALUES (:id, :sourceId, :processedValue, :reportDate)
                ON DUPLICATE KEY UPDATE
                  processed_value = VALUES(processed_value),
                  updated_at = NOW()
                """)
            .beanMapped()
            .build();
    }

    // ─── Step ─────────────────────────────────────────────────────────────────

    @Bean
    public Step <jobName>Step(
            JdbcPagingItemReader<<JobName>RawDto> reader,
            <JobName>Processor processor,
            JdbcBatchItemWriter<<JobName>OutputDto> writer) {

        return new StepBuilder("<jobName>Step", jobRepository)
            .<JobName>RawDto, <JobName>OutputDto>chunk(<chunk>, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
                .skip(DataAccessException.class)
                .skipLimit(100)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .backOffPolicy(new ExponentialBackOffPolicy())
            .listener(new <JobName>StepListener())
            .build();
    }

    // ─── Job ──────────────────────────────────────────────────────────────────

    @Bean
    public Job <jobName>Job(Step <jobName>Step) {
        return new JobBuilder("<jobName>Job", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(new <JobName>JobListener())
            .start(<jobName>Step)
            .build();
    }
}
```

### Step 3: Generate processor class
```java
@Component
@StepScope
@Slf4j
public class <JobName>Processor implements ItemProcessor<<JobName>RawDto, <JobName>OutputDto> {

    @Override
    public <JobName>OutputDto process(<JobName>RawDto raw) {
        // Return null to skip this item
        if (raw.status() == null) {
            log.warn("Skipping record {} — missing status", raw.id());
            return null;
        }
        return new <JobName>OutputDto(
            UUID.randomUUID().toString(),
            raw.id(),
            transformValue(raw),
            LocalDate.now()
        );
    }

    private String transformValue(<JobName>RawDto raw) {
        // transformation logic
        return raw.name().toUpperCase();
    }
}
```

### Step 4: Generate listeners
```java
@Slf4j
public class <JobName>JobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("[<JobName>Job] Starting — params: {}", jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("[<JobName>Job] Completed — {} items processed",
                jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount).sum());
        } else {
            log.error("[<JobName>Job] FAILED — status: {}", jobExecution.getStatus());
        }
    }
}
```

### Step 5: Generate scheduled trigger (if --schedule provided)
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class <JobName>Scheduler {

    private final JobLauncher jobLauncher;
    private final Job <jobName>Job;

    @Scheduled(cron = "${cdb.etl.<jobName>.cron:<schedule>}")
    public void run() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLocalDate("reportDate", LocalDate.now().minusDays(1))
            .addLong("runId", System.currentTimeMillis())
            .toJobParameters();
        log.info("[<JobName>] Scheduled trigger fired");
        jobLauncher.run(<jobName>Job, params);
    }
}
```

Add to `application.yml`:
```yaml
cdb:
  etl:
    <jobName>:
      cron: ${CDB_ETL_<JOBNAME>_CRON:<schedule>}
      enabled: ${CDB_ETL_<JOBNAME>_ENABLED:true}
```

### Step 6: Generate Spring Batch metadata table migration
```sql
-- Add to cdb-deployment/src/main/resources/database/<module>/ddl/v<next>.sql
-- Spring Batch requires its metadata tables (created automatically if spring.batch.jdbc.initialize-schema=always)
-- For production, use never and create manually:

-- (Include Spring Batch DDL for MySQL from spring-batch-core jar)
-- Or set: spring.batch.jdbc.initialize-schema=always for first deploy
```

Add to `application.yml`:
```yaml
spring:
  batch:
    jdbc:
      initialize-schema: ${CDB_BATCH_INIT_SCHEMA:never}
    job:
      enabled: false  # Don't auto-run jobs on startup
```

### Step 7: Report
- Files generated (config, processor, listeners, scheduler)
- Job parameters required for a run
- Chunk strategy chosen and why
- How to trigger: `POST /api/v1/etl/jobs/<jobName>Job/run` with body `{"reportDate": "2025-01-15"}`
- Scheduled cron (if configured)
- Target table DDL to pass to `/sb-migrate`
