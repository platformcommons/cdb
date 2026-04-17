# /etl-report — Generate Business Report (Excel/PDF/JSON)

Generate a complete reporting solution: aggregate SQL, Apache POI Excel, JasperReports PDF, and REST download endpoints for the CDB platform.

## Usage
```
/etl-report <ReportName> [--module <moduleName>] [--format excel|pdf|json|all] [--source <table>] [--group-by <column>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `ReportName` — PascalCase report name (e.g., `ProviderActivity`, `ApiUsageSummary`)
- `--module` — CDB module to add the report to
- `--format` — output format(s) to generate; `all` generates Excel + PDF + JSON
- `--source` — primary source table(s)
- `--group-by` — grouping column for aggregation (e.g., `status`, `category_id`, `month`)

### Step 1: Discover data model
1. Read the DDL migration files for the source table(s)
2. Read existing entity/DTO classes to understand available fields
3. Determine if the report needs aggregation or is a flat extract

### Step 2: Generate the aggregate SQL query

Write the query in a `@Repository` or service class using `NamedParameterJdbcTemplate`:

```java
@Repository
@RequiredArgsConstructor
public class <ReportName>ReportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<<ReportName>ReportRow> fetchReportData(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                p.id                                           AS source_id,
                p.name                                         AS source_name,
                p.status                                       AS status,
                c.name                                         AS category_name,
                DATE(p.created_at)                             AS activity_date,
                COUNT(DISTINCT a.id)                           AS api_count,
                SUM(CASE WHEN a.active = 1 THEN 1 ELSE 0 END) AS active_api_count,
                MAX(a.updated_at)                              AS last_activity
            FROM provider p
            LEFT JOIN category c       ON c.id = p.category_id AND c.active = 1
            LEFT JOIN api_definition a ON a.provider_id = p.id
            WHERE p.active = 1
              AND p.created_at >= :startDate
              AND p.created_at <  :endDate
            GROUP BY p.id, p.name, p.status, c.name, p.created_at
            ORDER BY p.created_at DESC, p.name
            LIMIT 10000
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, params, new <ReportName>RowMapper());
    }

    public List<<ReportName>Summary> fetchMonthlySummary(int months) {
        String sql = """
            SELECT
                DATE_FORMAT(p.created_at, '%Y-%m') AS month,
                p.status,
                COUNT(*)                            AS count
            FROM provider p
            WHERE p.active = 1
              AND p.created_at >= DATE_SUB(CURDATE(), INTERVAL :months MONTH)
            GROUP BY DATE_FORMAT(p.created_at, '%Y-%m'), p.status
            ORDER BY month DESC, status
            """;
        return jdbcTemplate.query(sql, Map.of("months", months), new <ReportName>SummaryRowMapper());
    }
}
```

### Step 3: Generate report row DTO and RowMapper
```java
public record <ReportName>ReportRow(
    String sourceId,
    String sourceName,
    String status,
    String categoryName,
    LocalDate activityDate,
    long apiCount,
    long activeApiCount,
    LocalDateTime lastActivity
) {}

public class <ReportName>RowMapper implements RowMapper<<ReportName>ReportRow> {
    @Override
    public <ReportName>ReportRow mapRow(ResultSet rs, int row) throws SQLException {
        return new <ReportName>ReportRow(
            rs.getString("source_id"),
            rs.getString("source_name"),
            rs.getString("status"),
            rs.getString("category_name"),
            rs.getDate("activity_date").toLocalDate(),
            rs.getLong("api_count"),
            rs.getLong("active_api_count"),
            rs.getTimestamp("last_activity") != null
                ? rs.getTimestamp("last_activity").toLocalDateTime() : null
        );
    }
}
```

### Step 4: Generate Excel service (Apache POI SXSSF)
```java
@Service
@RequiredArgsConstructor
public class <ReportName>ExcelService {

    private final <ReportName>ReportRepository reportRepository;

    public byte[] generate(LocalDate startDate, LocalDate endDate) throws IOException {
        List<<ReportName>ReportRow> rows = reportRepository.fetchReportData(startDate, endDate);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(500)) {
            Sheet sheet = workbook.createSheet("<ReportName>");
            sheet.createFreezePane(0, 1);

            // Header
            CellStyle headerStyle = buildHeaderStyle(workbook);
            String[] headers = {"ID", "Name", "Status", "Category", "Date", "API Count", "Active APIs", "Last Activity"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            // Data rows
            CellStyle dateStyle = buildDateStyle(workbook);
            int rowNum = 1;
            for (<ReportName>ReportRow row : rows) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(row.sourceId());
                dataRow.createCell(1).setCellValue(row.sourceName());
                dataRow.createCell(2).setCellValue(row.status());
                dataRow.createCell(3).setCellValue(row.categoryName());
                Cell dateCell = dataRow.createCell(4);
                dateCell.setCellValue(row.activityDate().toString());
                dataRow.createCell(5).setCellValue(row.apiCount());
                dataRow.createCell(6).setCellValue(row.activeApiCount());
                dataRow.createCell(7).setCellValue(
                    row.lastActivity() != null ? row.lastActivity().toString() : "");
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle buildDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }
}
```

### Step 5: Generate REST download controller
```java
@RestController
@RequestMapping("/api/v1/reports/<report-name>")
@RequiredArgsConstructor
@Tag(name = "<ReportName> Reports", description = "<ReportName> reporting endpoints")
public class <ReportName>ReportController {

    private final <ReportName>ExcelService excelService;
    private final <ReportName>ReportRepository reportRepository;

    @GetMapping("/excel")
    @Operation(summary = "Download <ReportName> report as Excel")
    @Cacheable(value = "<reportName>-excel", key = "#startDate.toString() + '-' + #endDate.toString()")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
            throws IOException {
        byte[] content = excelService.generate(startDate, endDate);
        String filename = "<report-name>-" + startDate + "-to-" + endDate + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(content);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get <ReportName> monthly summary for dashboard")
    public ResponseEntity<List<<ReportName>Summary>> getSummary(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(reportRepository.fetchMonthlySummary(months));
    }

    @GetMapping("/data")
    @Operation(summary = "Get raw report data as JSON")
    public ResponseEntity<List<<ReportName>ReportRow>> getData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportRepository.fetchReportData(startDate, endDate));
    }
}
```

### Step 6: Check and add Maven dependencies
```xml
<!-- Apache POI for Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### Step 7: Report
- All files generated (repository, service, controller, DTO, row mapper)
- API endpoints available: Excel download, JSON data, monthly summary
- SQL query and indexes needed (pass to `/sb-migrate add-index`)
- Cache configuration — which endpoints are cached and TTL
- Angular service call to add (pass to `/ng-service` to generate the frontend download button)
