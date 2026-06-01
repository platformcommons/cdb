package com.platformcommons.security.tests;

import com.platformcommons.security.model.HttpProbeResult;
import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.service.HttpClientService;
import com.platformcommons.security.service.ScreenshotService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A05:2021 – Security Misconfiguration
 *
 * Tests: exposed sensitive files, Apache status pages, CORS wildcard,
 * default Apache test page, error page information disclosure.
 */
@Component
public class MisconfigurationTest {

    // Files/paths that should never be publicly accessible
    private record SensitivePath(String path, String description, SeverityLevel severity) {}

    private static final List<SensitivePath> SENSITIVE_PATHS = List.of(
            new SensitivePath("/.git/config",           "Git repository configuration exposed",          SeverityLevel.CRITICAL),
            new SensitivePath("/.git/HEAD",             "Git HEAD ref exposed",                          SeverityLevel.HIGH),
            new SensitivePath("/.env",                  ".env environment file exposed",                 SeverityLevel.CRITICAL),
            new SensitivePath("/.htaccess",             ".htaccess configuration exposed",               SeverityLevel.HIGH),
            new SensitivePath("/.htpasswd",             ".htpasswd password file exposed",               SeverityLevel.CRITICAL),
            new SensitivePath("/web.config",            "web.config file exposed",                       SeverityLevel.HIGH),
            new SensitivePath("/phpinfo.php",           "phpinfo() output exposed",                      SeverityLevel.HIGH),
            new SensitivePath("/info.php",              "phpinfo() output exposed",                      SeverityLevel.HIGH),
            new SensitivePath("/server-status",         "Apache server-status page exposed",             SeverityLevel.HIGH),
            new SensitivePath("/server-info",           "Apache server-info page exposed",               SeverityLevel.HIGH),
            new SensitivePath("/README.md",             "README exposed – may disclose internals",       SeverityLevel.LOW),
            new SensitivePath("/CHANGELOG.md",          "Changelog exposes version history",             SeverityLevel.LOW),
            new SensitivePath("/composer.json",         "composer.json exposes dependency list",         SeverityLevel.MEDIUM),
            new SensitivePath("/package.json",          "package.json exposes dependency list",          SeverityLevel.MEDIUM),
            new SensitivePath("/Dockerfile",            "Dockerfile exposes infrastructure config",      SeverityLevel.MEDIUM),
            new SensitivePath("/docker-compose.yml",    "Docker Compose config exposed",                 SeverityLevel.MEDIUM),
            new SensitivePath("/backup.sql",            "SQL backup file exposed",                       SeverityLevel.CRITICAL),
            new SensitivePath("/db.sql",                "SQL dump file exposed",                         SeverityLevel.CRITICAL),
            new SensitivePath("/config.php",            "PHP config file may be readable",               SeverityLevel.HIGH),
            new SensitivePath("/.DS_Store",             ".DS_Store macOS metadata file exposed",         SeverityLevel.LOW)
    );

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public MisconfigurationTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        results.addAll(testSensitiveFiles(baseUrl));
        results.addAll(testCors(baseUrl, endpoints));
        results.addAll(testErrorPageDisclosure(baseUrl));
        results.addAll(testDefaultApachePage(baseUrl));

        return results;
    }

    // ─── sub-tests ───────────────────────────────────────────────────────────

    private List<TestResult> testSensitiveFiles(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        for (SensitivePath sp : SENSITIVE_PATHS) {
            String url = baseUrl + sp.path();
            HttpProbeResult probe = http.get(url);

            if (!probe.isError() && probe.getStatusCode() == 200) {
                results.add(TestResult.builder("Sensitive File Exposed: " + sp.path(),
                                OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                        .severity(sp.severity())
                        .endpoint(url)
                        .description(sp.description() + ". The file at " + sp.path()
                                + " is publicly accessible and returned HTTP 200.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "Exposed: " + sp.path()))
                        .passed(false)
                        .recommendation(buildRemediationForPath(sp.path()))
                        .build());
            }
        }

        if (results.isEmpty()) {
            results.add(TestResult.builder("Sensitive File Exposure", OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("None of the common sensitive files are accessible.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testCors(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        String target = baseUrl + (endpoints.isEmpty() ? "/" : endpoints.get(0));

        HttpProbeResult probe = http.get(target, java.util.Map.of("Origin", "https://evil-attacker.com"));
        if (probe.isError()) return results;

        String acaoHeader = probe.getHeader("Access-Control-Allow-Origin");
        String acac       = probe.getHeader("Access-Control-Allow-Credentials");

        if ("*".equals(acaoHeader)) {
            results.add(TestResult.builder("CORS Wildcard Origin", OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.HIGH)
                    .endpoint(target)
                    .description("Access-Control-Allow-Origin: * allows any origin to make cross-origin requests. "
                            + "Combined with credentials this can lead to account takeover.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "CORS Wildcard"))
                    .passed(false)
                    .recommendation("""
                            Replace the wildcard with an explicit allowed-origin list:

                            SetEnvIf Origin "https://yourdomain\\.com$" CORS_ALLOWED=1
                            Header always set Access-Control-Allow-Origin "%{CORS_ALLOWED_ORIGIN}e" env=CORS_ALLOWED
                            Header always set Access-Control-Allow-Credentials "true" env=CORS_ALLOWED

                            Never use Access-Control-Allow-Origin: * with credentials.""")
                    .build());
        } else if (acaoHeader != null && acaoHeader.contains("evil-attacker.com")) {
            results.add(TestResult.builder("CORS Origin Reflection", OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.HIGH)
                    .endpoint(target)
                    .description("The server reflects the arbitrary Origin header back in "
                            + "Access-Control-Allow-Origin. Any website can make authenticated cross-origin requests.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "CORS Reflection"))
                    .passed(false)
                    .recommendation("Maintain an explicit whitelist of allowed origins. "
                            + "Never echo the Origin request header back without validation.")
                    .build());
        } else {
            results.add(TestResult.builder("CORS Policy", OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.PASS)
                    .endpoint(target)
                    .description("CORS policy does not reflect arbitrary origins or use a wildcard.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testErrorPageDisclosure(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        // Request a non-existent path to trigger error pages
        String url404 = baseUrl + "/this-path-does-not-exist-owasptest-" + System.currentTimeMillis();
        HttpProbeResult probe = http.get(url404);

        if (probe.isError()) return results;

        String body = probe.getResponseBody() != null ? probe.getResponseBody().toLowerCase() : "";

        boolean stackTrace = body.contains("stack trace") || body.contains("at com.") || body.contains("at org.");
        boolean phpError   = body.contains("fatal error") || body.contains("warning:") || body.contains("notice:");
        boolean dbError    = body.contains("sql") || body.contains("mysql") || body.contains("ora-");

        if (stackTrace || phpError || dbError) {
            results.add(TestResult.builder("Error Page Information Disclosure",
                            OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.MEDIUM)
                    .endpoint(url404)
                    .description("The error page for a non-existent resource contains technical information "
                            + "(stack traces, PHP warnings, or database errors) that aids attackers.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "Error Disclosure"))
                    .passed(false)
                    .recommendation("""
                            Configure custom error pages in Apache2:

                            ErrorDocument 400 /errors/400.html
                            ErrorDocument 403 /errors/403.html
                            ErrorDocument 404 /errors/404.html
                            ErrorDocument 500 /errors/500.html

                            For PHP applications, set in php.ini or .htaccess:
                            php_flag display_errors Off
                            php_value error_reporting 0""")
                    .build());
        } else {
            results.add(TestResult.builder("Error Page Disclosure", OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("Error page does not reveal stack traces, PHP errors, or DBMS errors.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testDefaultApachePage(String baseUrl) {
        List<TestResult> results = new ArrayList<>();
        HttpProbeResult probe = http.get(baseUrl + "/");
        if (probe.isError()) return results;

        String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";
        boolean isDefaultPage = body.contains("Apache2 Ubuntu Default Page")
                || body.contains("It works!")
                || body.contains("Apache HTTP Server Test Page")
                || body.contains("Test Page for the Apache HTTP Server");

        if (isDefaultPage) {
            results.add(TestResult.builder("Default Apache Test Page Exposed",
                            OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.MEDIUM)
                    .endpoint(baseUrl + "/")
                    .description("The default Apache test page is visible. This indicates the server has not been "
                            + "properly configured and reveals that Apache is installed.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "Default Apache Page"))
                    .passed(false)
                    .recommendation("""
                            Remove or replace the default Apache test page:

                            sudo rm /var/www/html/index.html
                            # Replace with your application content

                            # Or disable the default virtual host:
                            sudo a2dissite 000-default.conf
                            sudo systemctl reload apache2""")
                    .build());
        } else {
            results.add(TestResult.builder("Default Apache Test Page", OWASPCategory.A05_SECURITY_MISCONFIGURATION)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl + "/")
                    .description("Default Apache test page is not visible.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private String buildRemediationForPath(String path) {
        if (path.startsWith("/.git")) {
            return """
                    Deny access to .git directory in Apache2:

                    <DirectoryMatch "\\.git">
                        Require all denied
                    </DirectoryMatch>

                    Or block via .htaccess:
                    RedirectMatch 404 /\\.git""";
        }
        if (path.equals("/.env")) {
            return """
                    Block .env file access:

                    <Files ".env">
                        Require all denied
                    </Files>

                    Never store .env files in the web root.""";
        }
        if (path.contains("server-status") || path.contains("server-info")) {
            return """
                    Restrict Apache status pages:

                    <Location /server-status>
                        Require local
                        # Or: Require ip 10.0.0.0/8
                    </Location>
                    <Location /server-info>
                        Require local
                    </Location>""";
        }
        if (path.contains(".sql") || path.contains("backup")) {
            return "Never store database dumps in the web root. "
                    + "Move backup files outside the DocumentRoot directory.";
        }
        return """
                Block sensitive files in Apache2:

                <FilesMatch "(\\.env|\\.htaccess|\\.htpasswd|composer\\.json|package\\.json|Dockerfile)$">
                    Require all denied
                </FilesMatch>""";
    }
}
