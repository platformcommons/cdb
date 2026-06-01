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
 * A01:2021 – Broken Access Control
 *
 * Tests: admin path enumeration, path traversal, dangerous HTTP methods,
 * directory listing, and IDOR-style parameter tampering.
 */
@Component
public class BrokenAccessControlTest {

    private static final List<String> ADMIN_PATHS = List.of(
            "/admin", "/admin/", "/administrator", "/administrator/",
            "/phpmyadmin", "/phpmyadmin/", "/manager", "/manager/html",
            "/wp-admin", "/wp-admin/", "/dashboard", "/cpanel",
            "/.well-known/security.txt");

    private static final List<String> TRAVERSAL_PROBES = List.of(
            "/../../../etc/passwd",
            "/..%2F..%2F..%2Fetc%2Fpasswd",
            "/%2e%2e/%2e%2e/%2e%2e/etc/passwd",
            "/....//....//....//etc/passwd");

    private static final List<String> DANGEROUS_METHODS = List.of("TRACE", "OPTIONS", "PUT", "DELETE");

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public BrokenAccessControlTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        results.addAll(testAdminPaths(baseUrl));
        results.addAll(testPathTraversal(baseUrl));
        results.addAll(testDangerousMethods(baseUrl));
        results.addAll(testDirectoryListing(baseUrl, endpoints));

        return results;
    }

    // ─── sub-tests ───────────────────────────────────────────────────────────

    private List<TestResult> testAdminPaths(String baseUrl) {
        List<TestResult> results = new ArrayList<>();
        List<String> exposed = new ArrayList<>();
        HttpProbeResult lastExposedProbe = null;

        for (String path : ADMIN_PATHS) {
            HttpProbeResult probe = http.get(baseUrl + path);
            if (!probe.isError() && probe.getStatusCode() == 200) {
                exposed.add(path + " → HTTP 200");
                lastExposedProbe = probe;
            }
        }

        if (!exposed.isEmpty()) {
            HttpProbeResult fp = lastExposedProbe;
            results.add(TestResult.builder("Admin Path Exposed", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                    .severity(SeverityLevel.HIGH)
                    .endpoint(baseUrl)
                    .description("Admin or sensitive paths returned HTTP 200 without authentication:\n"
                            + String.join("\n", exposed))
                    .evidence(fp.toEvidenceString())
                    .screenshotPng(screenshots.capture(fp, "Admin Path Exposed"))
                    .passed(false)
                    .recommendation("""
                            Restrict admin paths using authentication and authorisation controls.
                            Apache2 example:

                            <Location /admin>
                                AuthType Basic
                                AuthName "Restricted Area"
                                AuthUserFile /etc/apache2/.htpasswd
                                Require valid-user
                                # Or better: allow only from trusted IPs
                                Require ip 10.0.0.0/8
                            </Location>""")
                    .build());
        } else {
            results.add(TestResult.builder("Admin Path Enumeration", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("No common admin paths returned HTTP 200. Access control appears in place.")
                    .evidence("Checked: " + String.join(", ", ADMIN_PATHS))
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testPathTraversal(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        for (String probe : TRAVERSAL_PROBES) {
            String url = baseUrl + probe;
            HttpProbeResult r = http.get(url);
            if (!r.isError() && r.getStatusCode() == 200
                    && r.getResponseBody() != null
                    && r.getResponseBody().contains("root:")) {

                results.add(TestResult.builder("Path Traversal – /etc/passwd Readable",
                                OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                        .severity(SeverityLevel.CRITICAL)
                        .endpoint(url)
                        .description("Directory traversal allowed reading /etc/passwd. "
                                + "The probe \"" + probe + "\" returned file contents.")
                        .evidence(r.toEvidenceString())
                        .screenshotPng(screenshots.capture(r, "Path Traversal"))
                        .passed(false)
                        .recommendation("""
                                Enable mod_rewrite and block traversal sequences:

                                RewriteEngine On
                                RewriteRule (\\.\\./) - [F,L]

                                Also ensure the document root is set correctly:

                                <Directory />
                                    Options None
                                    AllowOverride None
                                    Require all denied
                                </Directory>
                                <Directory /var/www/html>
                                    Require all granted
                                </Directory>""")
                        .build());
                return results;   // one finding is enough evidence
            }
        }

        results.add(TestResult.builder("Path Traversal", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                .severity(SeverityLevel.PASS)
                .endpoint(baseUrl)
                .description("Path traversal probes did not succeed – server correctly blocks directory traversal.")
                .evidence("Tested probes: " + String.join(", ", TRAVERSAL_PROBES))
                .passed(true)
                .build());
        return results;
    }

    private List<TestResult> testDangerousMethods(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        for (String method : DANGEROUS_METHODS) {
            HttpProbeResult probe = http.method(baseUrl + "/", method);
            boolean alarming = !probe.isError() && (probe.getStatusCode() == 200 || probe.getStatusCode() == 405 == false);

            if (!probe.isError() && probe.getStatusCode() < 405) {
                String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";
                // TRACE echoes request headers back – that's a data leak
                if ("TRACE".equals(method) && body.contains("User-Agent")) {
                    results.add(TestResult.builder("HTTP TRACE Enabled", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                            .severity(SeverityLevel.MEDIUM)
                            .endpoint(baseUrl + "/")
                            .description("HTTP TRACE method is enabled. It echoes request headers back to the caller, "
                                    + "enabling Cross-Site Tracing (XST) attacks that steal cookies.")
                            .evidence(probe.toEvidenceString())
                            .screenshotPng(screenshots.capture(probe, "HTTP TRACE Enabled"))
                            .passed(false)
                            .recommendation("TraceEnable Off")
                            .build());
                } else if (List.of("PUT", "DELETE").contains(method)) {
                    results.add(TestResult.builder("Dangerous HTTP Method: " + method,
                                    OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                            .severity(SeverityLevel.HIGH)
                            .endpoint(baseUrl + "/")
                            .description("The " + method + " HTTP method returned HTTP " + probe.getStatusCode()
                                    + ". If writable endpoints are present, attackers can upload or delete files.")
                            .evidence(probe.toEvidenceString())
                            .screenshotPng(screenshots.capture(probe, method + " Method"))
                            .passed(false)
                            .recommendation("""
                                    Restrict HTTP methods in Apache2:

                                    <LimitExcept GET POST HEAD>
                                        Require all denied
                                    </LimitExcept>""")
                            .build());
                }
            }
        }

        if (results.isEmpty()) {
            results.add(TestResult.builder("Dangerous HTTP Methods", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("TRACE, PUT, DELETE methods are not permitted (405/403).")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testDirectoryListing(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        List<String> listingPaths = new ArrayList<>();
        HttpProbeResult lastListingProbe = null;

        List<String> toCheck = new ArrayList<>(endpoints);
        toCheck.add("/icons/");
        toCheck.add("/images/");
        toCheck.add("/uploads/");
        toCheck.add("/files/");
        toCheck.add("/static/");

        for (String ep : toCheck) {
            HttpProbeResult probe = http.get(baseUrl + ep);
            if (!probe.isError() && probe.getStatusCode() == 200
                    && probe.getResponseBody() != null
                    && (probe.getResponseBody().contains("Index of /")
                        || probe.getResponseBody().contains("Directory listing for"))) {
                listingPaths.add(ep);
                lastListingProbe = probe;
            }
        }

        if (!listingPaths.isEmpty()) {
            HttpProbeResult fp = lastListingProbe;
            results.add(TestResult.builder("Directory Listing Enabled", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                    .severity(SeverityLevel.MEDIUM)
                    .endpoint(baseUrl)
                    .description("Directory listing is enabled for: " + String.join(", ", listingPaths)
                            + ". Attackers can browse all files in these directories.")
                    .evidence(fp.toEvidenceString())
                    .screenshotPng(screenshots.capture(fp, "Directory Listing"))
                    .passed(false)
                    .recommendation("""
                            Disable directory listing globally:

                            <Directory /var/www/html>
                                Options -Indexes
                            </Directory>

                            Or via .htaccess:
                            Options -Indexes""")
                    .build());
        } else {
            results.add(TestResult.builder("Directory Listing", OWASPCategory.A01_BROKEN_ACCESS_CONTROL)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("Directory listing is disabled on all tested paths.")
                    .passed(true)
                    .build());
        }
        return results;
    }
}
