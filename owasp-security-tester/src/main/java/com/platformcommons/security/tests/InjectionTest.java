package com.platformcommons.security.tests;

import com.platformcommons.security.model.HttpProbeResult;
import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.service.HttpClientService;
import com.platformcommons.security.service.ScreenshotService;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A03:2021 – Injection
 *
 * Tests: reflected XSS, SQL injection error detection, template injection,
 * and basic command injection indicators.
 *
 * NOTE: Probes send benign detection payloads only. No destructive payloads are used.
 */
@Component
public class InjectionTest {

    // XSS detection: look for the literal payload reflected back
    private static final String XSS_PROBE   = "<script>alert('OWASPT')</script>";
    // SQLi: look for DBMS error messages in the response
    private static final String SQLI_PROBE  = "' OR '1'='1";
    private static final String SQLI_PROBE2 = "1\" OR \"1\"=\"1";
    // SSTI detection: arithmetic expression that should evaluate to 49
    private static final String SSTI_PROBE  = "{{7*7}}";
    // Cmd: look for command interpreter error strings in response
    private static final String CMD_PROBE   = ";id;";

    // DBMS error strings to detect in responses
    private static final List<String> SQL_ERROR_PATTERNS = List.of(
            "sql syntax", "mysql_fetch", "ORA-0", "pg_query()", "SQLite3::",
            "unclosed quotation mark", "quoted string not properly terminated",
            "you have an error in your sql", "warning: mysql", "supplied argument is not a valid mysql"
    );

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public InjectionTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        for (String ep : endpoints) {
            String base = baseUrl + ep;
            results.addAll(testXss(base));
            results.addAll(testSqli(base));
            results.addAll(testSsti(base));
            results.addAll(testCmdInjection(base));
        }

        return results;
    }

    // ─── sub-tests ───────────────────────────────────────────────────────────

    private List<TestResult> testXss(String endpointUrl) {
        List<TestResult> results = new ArrayList<>();

        String encoded = URLEncoder.encode(XSS_PROBE, StandardCharsets.UTF_8);
        // Try common parameter names
        for (String param : List.of("q", "search", "query", "name", "input", "s")) {
            String url = endpointUrl + (endpointUrl.contains("?") ? "&" : "?") + param + "=" + encoded;
            HttpProbeResult probe = http.get(url);

            if (probe.isError()) continue;

            String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";
            // Reflected XSS: unencoded payload appears in response body
            if (body.contains("<script>alert(") || body.contains("alert('OWASPT')")) {
                results.add(TestResult.builder("Reflected XSS – Parameter: " + param,
                                OWASPCategory.A03_INJECTION)
                        .severity(SeverityLevel.HIGH)
                        .endpoint(url)
                        .description("Reflected XSS detected: the script payload was echoed back unencoded in the "
                                + "HTTP response for parameter '" + param + "'.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "Reflected XSS"))
                        .passed(false)
                        .recommendation("""
                                Enable Apache2's mod_security or implement output encoding:

                                1. Always HTML-encode output in your application layer.
                                2. Enable Content-Security-Policy header:
                                   Header always set Content-Security-Policy "default-src 'self'"
                                3. Enable mod_security WAF:
                                   SecRuleEngine On
                                   SecRule ARGS "<script" "id:1001,phase:2,block,msg:'XSS Attack'"
                                4. Use a framework that auto-escapes output (e.g. Thymeleaf, JSP EL).""")
                        .build());
                return results;
            }
        }

        results.add(TestResult.builder("Reflected XSS", OWASPCategory.A03_INJECTION)
                .severity(SeverityLevel.PASS)
                .endpoint(endpointUrl)
                .description("XSS probe was not reflected unencoded in the response.")
                .passed(true)
                .build());
        return results;
    }

    private List<TestResult> testSqli(String endpointUrl) {
        List<TestResult> results = new ArrayList<>();

        for (String probe : List.of(SQLI_PROBE, SQLI_PROBE2)) {
            String encoded = URLEncoder.encode(probe, StandardCharsets.UTF_8);
            for (String param : List.of("id", "user", "username", "search", "q")) {
                String url = endpointUrl + (endpointUrl.contains("?") ? "&" : "?") + param + "=" + encoded;
                HttpProbeResult r = http.get(url);
                if (r.isError()) continue;

                String body = (r.getResponseBody() != null ? r.getResponseBody() : "").toLowerCase();
                boolean hasError = SQL_ERROR_PATTERNS.stream().anyMatch(body::contains);

                if (hasError) {
                    results.add(TestResult.builder("SQL Injection – DBMS Error Disclosure",
                                    OWASPCategory.A03_INJECTION)
                            .severity(SeverityLevel.CRITICAL)
                            .endpoint(url)
                            .description("A DBMS error message was returned when a SQL injection probe was sent "
                                    + "in the '" + param + "' parameter. This strongly indicates a SQL injection vulnerability.")
                            .evidence(r.toEvidenceString())
                            .screenshotPng(screenshots.capture(r, "SQL Injection"))
                            .passed(false)
                            .recommendation("""
                                    Fix in application code:
                                    - Use parameterised queries / prepared statements for ALL database interactions.
                                    - Never concatenate user input into SQL strings.
                                    - Apply the principle of least privilege to the database account.

                                    Apache2 WAF layer (mod_security):
                                    SecRuleEngine On
                                    SecRule ARGS "(?i)(\\bselect\\b|\\bunion\\b|\\bdrop\\b|\\binsert\\b|')" \\
                                        "id:1002,phase:2,block,msg:'SQL Injection'"

                                    Also ensure PHP/app errors are not displayed to users:
                                    php_flag display_errors Off""")
                            .build());
                    return results;
                }
            }
        }

        results.add(TestResult.builder("SQL Injection", OWASPCategory.A03_INJECTION)
                .severity(SeverityLevel.PASS)
                .endpoint(endpointUrl)
                .description("SQL injection probes did not trigger DBMS error messages in responses.")
                .passed(true)
                .build());
        return results;
    }

    private List<TestResult> testSsti(String endpointUrl) {
        List<TestResult> results = new ArrayList<>();
        String encoded = URLEncoder.encode(SSTI_PROBE, StandardCharsets.UTF_8);

        for (String param : List.of("name", "template", "page", "lang")) {
            String url = endpointUrl + (endpointUrl.contains("?") ? "&" : "?") + param + "=" + encoded;
            HttpProbeResult probe = http.get(url);
            if (probe.isError()) continue;

            String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";
            // If the template evaluates {{7*7}} → 49 appears in response
            if (body.contains("49") && !body.contains("{{7*7}}")) {
                results.add(TestResult.builder("Server-Side Template Injection (SSTI)",
                                OWASPCategory.A03_INJECTION)
                        .severity(SeverityLevel.CRITICAL)
                        .endpoint(url)
                        .description("Template expression {{7*7}} was evaluated server-side to 49. "
                                + "SSTI can lead to Remote Code Execution.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "SSTI"))
                        .passed(false)
                        .recommendation("""
                                Never pass raw user input into template rendering calls.
                                Use a sandboxed template context or escaping:
                                - Jinja2: use |e filter or mark variables safe explicitly.
                                - Thymeleaf: use [[${var}]] (auto-escaped) not [(${var})].
                                - Freemarker: disable unsafe built-ins via Configuration.setNewBuiltinClassResolver().""")
                        .build());
                return results;
            }
        }

        results.add(TestResult.builder("Server-Side Template Injection", OWASPCategory.A03_INJECTION)
                .severity(SeverityLevel.PASS)
                .endpoint(endpointUrl)
                .description("SSTI probe {{7*7}} was not evaluated in responses.")
                .passed(true)
                .build());
        return results;
    }

    private List<TestResult> testCmdInjection(String endpointUrl) {
        List<TestResult> results = new ArrayList<>();
        String encoded = URLEncoder.encode(CMD_PROBE, StandardCharsets.UTF_8);

        for (String param : List.of("cmd", "exec", "command", "ping", "host")) {
            String url = endpointUrl + (endpointUrl.contains("?") ? "&" : "?") + param + "=" + encoded;
            HttpProbeResult probe = http.get(url);
            if (probe.isError()) continue;

            String body = (probe.getResponseBody() != null ? probe.getResponseBody() : "").toLowerCase();
            // Look for typical Unix id command output
            if (body.contains("uid=") && body.contains("gid=")) {
                results.add(TestResult.builder("OS Command Injection",
                                OWASPCategory.A03_INJECTION)
                        .severity(SeverityLevel.CRITICAL)
                        .endpoint(url)
                        .description("The response contains Unix id command output (uid=, gid=) after injecting ';id;' "
                                + "into the '" + param + "' parameter. Remote code execution is possible.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "Command Injection"))
                        .passed(false)
                        .recommendation("""
                                Never pass user-supplied data to system(), exec(), shell_exec() or similar.
                                If OS commands are required:
                                - Whitelist allowed values (never a freeform string).
                                - Use language-native APIs instead of shell commands.
                                - Run the web process with minimal OS privileges (dedicated user, no shell).
                                - Use AppArmor / SELinux profiles to restrict what the web process can execute.""")
                        .build());
                return results;
            }
        }

        results.add(TestResult.builder("OS Command Injection", OWASPCategory.A03_INJECTION)
                .severity(SeverityLevel.PASS)
                .endpoint(endpointUrl)
                .description("Command injection probe did not trigger command output in responses.")
                .passed(true)
                .build());
        return results;
    }
}
