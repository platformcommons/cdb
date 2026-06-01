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
 * A05:2021 – Security Misconfiguration | A02:2021 – Cryptographic Failures
 *
 * Checks for missing / misconfigured HTTP security response headers.
 */
@Component
public class SecurityHeadersTest {

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public SecurityHeadersTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        String targetUrl = endpoints.isEmpty() ? baseUrl : baseUrl + endpoints.get(0);

        HttpProbeResult probe = http.get(targetUrl);

        results.add(checkHeader(probe, "Content-Security-Policy",
                "CSP Missing",
                "No Content-Security-Policy header found. Attackers can inject malicious scripts (XSS).",
                SeverityLevel.HIGH,
                "Header always set Content-Security-Policy \"default-src 'self'; script-src 'self'; object-src 'none';\"",
                OWASPCategory.A05_SECURITY_MISCONFIGURATION));

        results.add(checkHeader(probe, "X-Frame-Options",
                "X-Frame-Options Missing",
                "No X-Frame-Options header. The page can be embedded in an iframe enabling clickjacking attacks.",
                SeverityLevel.MEDIUM,
                "Header always set X-Frame-Options \"SAMEORIGIN\"",
                OWASPCategory.A05_SECURITY_MISCONFIGURATION));

        results.add(checkHeader(probe, "X-Content-Type-Options",
                "X-Content-Type-Options Missing",
                "No X-Content-Type-Options header. MIME-type sniffing can allow XSS via crafted content.",
                SeverityLevel.MEDIUM,
                "Header always set X-Content-Type-Options \"nosniff\"",
                OWASPCategory.A05_SECURITY_MISCONFIGURATION));

        results.add(checkHeader(probe, "Referrer-Policy",
                "Referrer-Policy Missing",
                "No Referrer-Policy header. Sensitive URL parameters can leak to third parties via the Referer header.",
                SeverityLevel.LOW,
                "Header always set Referrer-Policy \"strict-origin-when-cross-origin\"",
                OWASPCategory.A05_SECURITY_MISCONFIGURATION));

        results.add(checkHeader(probe, "Permissions-Policy",
                "Permissions-Policy Missing",
                "No Permissions-Policy header. Browser features (camera, microphone, geolocation) may be available to malicious scripts.",
                SeverityLevel.LOW,
                "Header always set Permissions-Policy \"camera=(), microphone=(), geolocation=(), interest-cohort=()\"",
                OWASPCategory.A05_SECURITY_MISCONFIGURATION));

        // HSTS only meaningful on HTTPS
        if (targetUrl.startsWith("https://")) {
            results.add(checkHeader(probe, "Strict-Transport-Security",
                    "HSTS Missing",
                    "No Strict-Transport-Security header on HTTPS endpoint. Browsers may permit downgrade to HTTP.",
                    SeverityLevel.HIGH,
                    "Header always set Strict-Transport-Security \"max-age=31536000; includeSubDomains; preload\"",
                    OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES));
        }

        // Server version disclosure
        String serverHeader = probe.getHeader("Server");
        if (serverHeader != null && serverHeader.matches(".*[0-9]+\\.[0-9]+.*")) {
            results.add(TestResult.builder("Server Version Disclosure", OWASPCategory.A06_VULNERABLE_COMPONENTS)
                    .severity(SeverityLevel.MEDIUM)
                    .endpoint(targetUrl)
                    .description("The Server header discloses the exact Apache version: \"" + serverHeader
                            + "\". This aids attackers in targeting known CVEs for that version.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "Server Version Disclosure"))
                    .passed(false)
                    .recommendation("ServerTokens Prod\nServerSignature Off")
                    .build());
        } else if (serverHeader == null) {
            results.add(TestResult.builder("Server Header Absent", OWASPCategory.A06_VULNERABLE_COMPONENTS)
                    .severity(SeverityLevel.PASS)
                    .endpoint(targetUrl)
                    .description("No Server header in response – version information is not disclosed.")
                    .evidence(probe.toEvidenceString())
                    .passed(true)
                    .recommendation("")
                    .build());
        }

        // X-Powered-By
        String poweredBy = probe.getHeader("X-Powered-By");
        if (poweredBy != null) {
            results.add(TestResult.builder("X-Powered-By Disclosure", OWASPCategory.A06_VULNERABLE_COMPONENTS)
                    .severity(SeverityLevel.LOW)
                    .endpoint(targetUrl)
                    .description("X-Powered-By header exposes technology: \"" + poweredBy + "\".")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "X-Powered-By Disclosure"))
                    .passed(false)
                    .recommendation("Header unset X-Powered-By\nHeader always unset X-Powered-By")
                    .build());
        }

        return results;
    }

    private TestResult checkHeader(HttpProbeResult probe, String headerName, String testName,
                                   String description, SeverityLevel severity,
                                   String apacheFix, OWASPCategory category) {
        String value = probe.getHeader(headerName);
        boolean present = value != null && !value.isBlank();

        return TestResult.builder(testName, category)
                .severity(present ? SeverityLevel.PASS : severity)
                .endpoint(probe.getUrl())
                .description(present
                        ? headerName + " is set: \"" + value + "\""
                        : description)
                .evidence(probe.toEvidenceString())
                .screenshotPng(present ? null : screenshots.capture(probe, testName))
                .passed(present)
                .recommendation(present ? "" : buildApacheFix(apacheFix))
                .build();
    }

    private String buildApacheFix(String directive) {
        return "<IfModule mod_headers.c>\n    " + directive + "\n</IfModule>";
    }
}
