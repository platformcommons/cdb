package com.platformcommons.security.tests;

import com.platformcommons.security.model.HttpProbeResult;
import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.service.HttpClientService;
import com.platformcommons.security.service.ScreenshotService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A06:2021 – Vulnerable and Outdated Components
 *
 * Detects version numbers in response headers and body,
 * maps them to known end-of-life / high-risk versions.
 */
@Component
public class VulnerableComponentsTest {

    // Apache end-of-life versions (anything below 2.4.x is EOL)
    private static final Pattern APACHE_VER = Pattern.compile("Apache/([0-9]+\\.([0-9]+)\\.([0-9]+))");
    // PHP EOL: anything below 8.1 as of 2024
    private static final Pattern PHP_VER    = Pattern.compile("PHP/([0-9]+)\\.([0-9]+)");
    // OpenSSL known vulnerable patterns
    private static final Pattern OPENSSL_VER = Pattern.compile("OpenSSL/([0-9]+\\.[0-9]+\\.[0-9]+[a-z]?)");
    // Common JS library versions in HTML
    private static final Pattern JQUERY_VER  = Pattern.compile("jquery[.-]([0-9]+\\.[0-9]+\\.[0-9]+)");
    private static final Pattern BOOTSTRAP_VER = Pattern.compile("bootstrap[.-]([0-9]+\\.[0-9]+\\.[0-9]+)");

    // Map of product → minimum acceptable minor.patch (simplified rule)
    private record VersionRule(String product, int minMajor, int minMinor, String eolNote) {}

    private static final List<VersionRule> RULES = List.of(
            new VersionRule("Apache", 2, 4,   "Apache 2.2 is EOL. Upgrade to Apache 2.4.x."),
            new VersionRule("PHP",    8, 1,    "PHP versions below 8.1 are EOL. Upgrade to PHP 8.2+."),
            new VersionRule("jQuery", 3, 6,    "jQuery below 3.6 has known XSS vulnerabilities. Upgrade.")
    );

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public VulnerableComponentsTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        Map<String, String> detected = new LinkedHashMap<>();

        String target = baseUrl + (endpoints.isEmpty() ? "/" : endpoints.get(0));
        HttpProbeResult probe = http.get(target);

        if (probe.isError()) return results;

        // ── Scan response headers ──────────────────────────────────────────
        String serverHeader  = probe.getHeader("Server");
        String xPoweredBy    = probe.getHeader("X-Powered-By");
        String viaHeader     = probe.getHeader("Via");

        scanHeader(serverHeader,  APACHE_VER,  "Apache",  detected);
        scanHeader(serverHeader,  PHP_VER,     "PHP",     detected);
        scanHeader(serverHeader,  OPENSSL_VER, "OpenSSL", detected);
        scanHeader(xPoweredBy,    PHP_VER,     "PHP",     detected);

        // ── Scan response body for JS library versions ─────────────────────
        String body = probe.getResponseBody() != null ? probe.getResponseBody().toLowerCase() : "";
        scanBody(body, JQUERY_VER,    "jQuery",    detected);
        scanBody(body, BOOTSTRAP_VER, "Bootstrap", detected);

        // ── Evaluate detections ────────────────────────────────────────────
        for (Map.Entry<String, String> entry : detected.entrySet()) {
            String component = entry.getKey();
            String version   = entry.getValue();

            results.add(buildFinding(probe, component, version));
        }

        if (results.isEmpty()) {
            results.add(TestResult.builder("Vulnerable Components", OWASPCategory.A06_VULNERABLE_COMPONENTS)
                    .severity(SeverityLevel.PASS)
                    .endpoint(target)
                    .description("No component version information was detectable in headers or page source. "
                            + "Consider this a partial check – a full SCA scan of source code is recommended.")
                    .passed(true)
                    .build());
        }

        return results;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void scanHeader(String headerValue, Pattern pattern, String label, Map<String, String> out) {
        if (headerValue == null) return;
        Matcher m = pattern.matcher(headerValue);
        if (m.find()) out.put(label, m.group(1));
    }

    private void scanBody(String body, Pattern pattern, String label, Map<String, String> out) {
        if (body == null) return;
        Matcher m = pattern.matcher(body);
        if (m.find()) out.put(label, m.group(1));
    }

    private TestResult buildFinding(HttpProbeResult probe, String component, String version) {
        String testName   = component + " Version Disclosed: " + version;
        String[] parts    = version.split("\\.");
        int major = parts.length > 0 ? safeInt(parts[0]) : 0;
        int minor = parts.length > 1 ? safeInt(parts[1]) : 0;

        VersionRule rule = RULES.stream()
                .filter(r -> r.product().equalsIgnoreCase(component))
                .findFirst().orElse(null);

        boolean isEol = rule != null && (major < rule.minMajor()
                || (major == rule.minMajor() && minor < rule.minMinor()));

        SeverityLevel severity = isEol ? SeverityLevel.HIGH : SeverityLevel.MEDIUM;

        String description = component + " version " + version + " is disclosed in HTTP response headers or source. "
                + (isEol ? rule.eolNote() : "Even non-EOL versions should not be disclosed; fingerprinting aids attackers.");

        String recommendation = buildRemediation(component);

        return TestResult.builder(testName, OWASPCategory.A06_VULNERABLE_COMPONENTS)
                .severity(severity)
                .endpoint(probe.getUrl())
                .description(description)
                .evidence(probe.toEvidenceString())
                .screenshotPng(screenshots.capture(probe, testName))
                .passed(false)
                .recommendation(recommendation)
                .build();
    }

    private String buildRemediation(String component) {
        return switch (component.toLowerCase()) {
            case "apache" -> """
                    Hide Apache version and update if EOL:

                    1. In /etc/apache2/conf-available/security.conf:
                       ServerTokens Prod
                       ServerSignature Off

                    2. Keep Apache updated:
                       sudo apt update && sudo apt upgrade apache2

                    3. Subscribe to Apache security announcements:
                       https://httpd.apache.org/security/vulnerabilities_24.html""";

            case "php" -> """
                    Hide PHP version and upgrade if EOL:

                    1. In php.ini:
                       expose_php = Off

                    2. Upgrade to a supported PHP version (8.2+ recommended):
                       sudo apt install php8.2

                    3. Check EOL status at https://www.php.net/supported-versions.php""";

            case "jquery" -> """
                    Update jQuery to the latest stable version (3.7+):

                    npm install jquery@latest

                    Or via CDN with Subresource Integrity:
                    <script src="https://code.jquery.com/jquery-3.7.1.min.js"
                            integrity="sha256-..." crossorigin="anonymous"></script>

                    Check for known jQuery CVEs at https://snyk.io/vuln/npm:jquery""";

            default -> "Update " + component + " to the latest stable release and "
                    + "suppress version disclosure in HTTP headers.";
        };
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
