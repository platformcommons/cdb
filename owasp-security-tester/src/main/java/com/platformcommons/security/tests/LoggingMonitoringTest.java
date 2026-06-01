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
 * A09:2021 – Security Logging and Monitoring Failures
 *
 * Checks externally visible signs of missing logging / monitoring:
 * detailed stack traces, no rate-limiting headers, missing security.txt.
 */
@Component
public class LoggingMonitoringTest {

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public LoggingMonitoringTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        results.addAll(testStackTraceExposure(baseUrl));
        results.addAll(testRateLimitingHeaders(baseUrl, endpoints));
        results.addAll(testSecurityTxt(baseUrl));

        return results;
    }

    private List<TestResult> testStackTraceExposure(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        // Trigger a 500 by requesting a path likely to cause an error
        String url = baseUrl + "/error-trigger-owasptest?param=<invalid>";
        HttpProbeResult probe = http.get(url);
        if (probe.isError()) return results;

        String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";
        boolean hasTrace = body.contains("at com.") || body.contains("at org.")
                || body.contains("java.lang.") || body.contains("Exception")
                || body.contains("Traceback (most recent call last)")  // Python
                || body.contains("stack trace");

        if (hasTrace && probe.getStatusCode() >= 500) {
            results.add(TestResult.builder("Stack Trace Exposed in Error Response",
                            OWASPCategory.A09_LOGGING_FAILURES)
                    .severity(SeverityLevel.MEDIUM)
                    .endpoint(url)
                    .description("The server returned a stack trace in the HTTP 500 response. "
                            + "Stack traces reveal internal class names, library versions, and file paths "
                            + "that assist attackers in crafting targeted exploits.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "Stack Trace Exposure"))
                    .passed(false)
                    .recommendation("""
                            Configure custom error pages and disable debug output in production:

                            Apache2 – /etc/apache2/conf-available/security.conf:
                            ErrorDocument 500 /errors/500.html

                            Spring Boot – application.properties:
                            server.error.include-stacktrace=never
                            server.error.include-message=never
                            server.error.include-exception=false

                            PHP – php.ini:
                            display_errors = Off
                            log_errors = On
                            error_log = /var/log/php_errors.log""")
                    .build());
        } else {
            results.add(TestResult.builder("Stack Trace Exposure", OWASPCategory.A09_LOGGING_FAILURES)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("Error responses do not appear to contain stack traces.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testRateLimitingHeaders(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        String target = baseUrl + (endpoints.isEmpty() ? "/" : endpoints.get(0));
        HttpProbeResult probe = http.get(target);
        if (probe.isError()) return results;

        boolean hasRateLimit = probe.getHeader("X-RateLimit-Limit") != null
                || probe.getHeader("RateLimit-Limit") != null
                || probe.getHeader("Retry-After") != null;

        if (!hasRateLimit) {
            results.add(TestResult.builder("No Rate-Limiting Headers Detected",
                            OWASPCategory.A09_LOGGING_FAILURES)
                    .severity(SeverityLevel.LOW)
                    .endpoint(target)
                    .description("No rate-limiting response headers (X-RateLimit-Limit, Retry-After) were found. "
                            + "Without rate limiting, brute-force and credential stuffing attacks are harder to detect or block.")
                    .evidence(probe.toEvidenceString())
                    .passed(false)
                    .recommendation("""
                            Implement rate limiting at the Apache2 / application layer:

                            Using mod_ratelimit (Apache 2.4+):
                            <Location /login>
                                SetOutputFilter RATE_LIMIT
                                SetEnv rate-limit 400
                            </Location>

                            Or use fail2ban to block repeated 401/403 responses:
                            Install fail2ban and configure /etc/fail2ban/jail.local:
                            [apache-auth]
                            enabled  = true
                            port     = http,https
                            logpath  = /var/log/apache2/error.log
                            maxretry = 5
                            bantime  = 3600""")
                    .build());
        } else {
            results.add(TestResult.builder("Rate-Limiting Headers", OWASPCategory.A09_LOGGING_FAILURES)
                    .severity(SeverityLevel.PASS)
                    .endpoint(target)
                    .description("Rate-limiting headers are present in the response.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testSecurityTxt(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        for (String path : List.of("/.well-known/security.txt", "/security.txt")) {
            HttpProbeResult probe = http.get(baseUrl + path);
            if (!probe.isError() && probe.getStatusCode() == 200
                    && probe.getResponseBody() != null
                    && probe.getResponseBody().toLowerCase().contains("contact:")) {
                results.add(TestResult.builder("security.txt Present", OWASPCategory.A09_LOGGING_FAILURES)
                        .severity(SeverityLevel.PASS)
                        .endpoint(baseUrl + path)
                        .description("A security.txt file is present at " + path
                                + ". Researchers can report vulnerabilities via the published contact.")
                        .passed(true)
                        .build());
                return results;
            }
        }

        results.add(TestResult.builder("security.txt Missing", OWASPCategory.A09_LOGGING_FAILURES)
                .severity(SeverityLevel.INFO)
                .endpoint(baseUrl)
                .description("No security.txt file found at /.well-known/security.txt or /security.txt. "
                        + "This file helps security researchers contact you to report vulnerabilities.")
                .passed(false)
                .recommendation("""
                        Create /.well-known/security.txt (RFC 9116):

                        Contact: mailto:security@yourdomain.com
                        Expires: 2026-01-01T00:00:00.000Z
                        Preferred-Languages: en
                        Policy: https://yourdomain.com/security-policy

                        Generate at https://securitytxt.org/""")
                .build());
        return results;
    }
}
