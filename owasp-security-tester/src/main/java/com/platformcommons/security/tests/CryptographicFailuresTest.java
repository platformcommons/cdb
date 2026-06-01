package com.platformcommons.security.tests;

import com.platformcommons.security.model.HttpProbeResult;
import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.service.HttpClientService;
import com.platformcommons.security.service.ScreenshotService;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A02:2021 – Cryptographic Failures
 *
 * Tests: HTTP→HTTPS redirect, HSTS, cookie security flags,
 * weak TLS protocols, sensitive data in responses.
 */
@Component
public class CryptographicFailuresTest {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CC_PATTERN =
            Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b");
    private static final Pattern API_KEY_PATTERN =
            Pattern.compile("(?i)(api[_-]?key|apikey|secret|token|password)\\s*[:=]\\s*['\"]?([\\w\\-]{16,})['\"]?");

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public CryptographicFailuresTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        results.addAll(testHttpsRedirect(baseUrl));
        results.addAll(testCookieFlags(baseUrl, endpoints));
        results.addAll(testSensitiveDataInResponse(baseUrl, endpoints));
        if (baseUrl.startsWith("https://")) {
            results.addAll(testTlsProtocols(baseUrl));
        }

        return results;
    }

    // ─── sub-tests ───────────────────────────────────────────────────────────

    private List<TestResult> testHttpsRedirect(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        if (baseUrl.startsWith("http://")) {
            HttpProbeResult probe = http.getNoFollow(baseUrl);
            boolean redirectsToHttps = !probe.isError()
                    && probe.isRedirect()
                    && probe.getHeader("Location") != null
                    && probe.getHeader("Location").startsWith("https://");

            if (!redirectsToHttps) {
                results.add(TestResult.builder("HTTP to HTTPS Redirect Missing",
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.HIGH)
                        .endpoint(baseUrl)
                        .description("The server does not redirect HTTP traffic to HTTPS. "
                                + "Data in transit (credentials, session tokens) is transmitted in cleartext.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "HTTP→HTTPS Redirect"))
                        .passed(false)
                        .recommendation("""
                                Force HTTPS redirect in Apache2:

                                <VirtualHost *:80>
                                    ServerName yourdomain.com
                                    RewriteEngine On
                                    RewriteCond %{HTTPS} off
                                    RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
                                </VirtualHost>

                                Or via .htaccess:
                                RewriteEngine On
                                RewriteCond %{HTTPS} !=on
                                RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]""")
                        .build());
            } else {
                results.add(TestResult.builder("HTTP to HTTPS Redirect", OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.PASS)
                        .endpoint(baseUrl)
                        .description("HTTP requests are redirected to HTTPS.")
                        .evidence(probe.toEvidenceString())
                        .passed(true)
                        .build());
            }
        }
        return results;
    }

    private List<TestResult> testCookieFlags(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        String target = baseUrl + (endpoints.isEmpty() ? "/" : endpoints.get(0));

        HttpProbeResult probe = http.get(target);
        if (probe.isError()) return results;

        probe.getResponseHeaders().forEach((headerName, headerValue) -> {
            if (!"set-cookie".equalsIgnoreCase(headerName)) return;

            String cookieName = headerValue.split("=")[0].trim();
            boolean hasSecure   = headerValue.toLowerCase().contains("secure");
            boolean hasHttpOnly = headerValue.toLowerCase().contains("httponly");
            boolean hasSameSite = headerValue.toLowerCase().contains("samesite");

            if (!hasSecure && baseUrl.startsWith("https://")) {
                results.add(TestResult.builder("Cookie Missing Secure Flag: " + cookieName,
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.MEDIUM)
                        .endpoint(target)
                        .description("Cookie \"" + cookieName + "\" lacks the Secure flag. "
                                + "It can be transmitted over unencrypted HTTP connections.")
                        .evidence("Set-Cookie: " + headerValue)
                        .screenshotPng(screenshots.capture(probe, "Cookie Secure Flag"))
                        .passed(false)
                        .recommendation("Header edit Set-Cookie (.*) \"$1; Secure; HttpOnly; SameSite=Strict\"")
                        .build());
            }

            if (!hasHttpOnly) {
                results.add(TestResult.builder("Cookie Missing HttpOnly Flag: " + cookieName,
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.MEDIUM)
                        .endpoint(target)
                        .description("Cookie \"" + cookieName + "\" lacks the HttpOnly flag. "
                                + "JavaScript can read this cookie, enabling session theft via XSS.")
                        .evidence("Set-Cookie: " + headerValue)
                        .screenshotPng(screenshots.capture(probe, "Cookie HttpOnly Flag"))
                        .passed(false)
                        .recommendation("Header edit Set-Cookie (.*) \"$1; HttpOnly\"")
                        .build());
            }

            if (!hasSameSite) {
                results.add(TestResult.builder("Cookie Missing SameSite Attribute: " + cookieName,
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.LOW)
                        .endpoint(target)
                        .description("Cookie \"" + cookieName + "\" has no SameSite attribute. "
                                + "Cross-site request forgery (CSRF) attacks may exploit this cookie.")
                        .evidence("Set-Cookie: " + headerValue)
                        .passed(false)
                        .recommendation("Header edit Set-Cookie (.*) \"$1; SameSite=Strict\"")
                        .build());
            }
        });

        if (results.isEmpty()) {
            results.add(TestResult.builder("Cookie Security Flags", OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                    .severity(SeverityLevel.PASS)
                    .endpoint(target)
                    .description("All cookies have Secure, HttpOnly, and SameSite attributes set.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testSensitiveDataInResponse(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        for (String ep : endpoints) {
            String url = baseUrl + ep;
            HttpProbeResult probe = http.get(url);
            if (probe.isError() || probe.getResponseBody() == null) continue;

            String body = probe.getResponseBody();

            if (EMAIL_PATTERN.matcher(body).find()) {
                results.add(TestResult.builder("Sensitive Data: Email Address Exposure",
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.LOW)
                        .endpoint(url)
                        .description("Email address(es) detected in the HTTP response body of " + ep + ". "
                                + "Ensure email addresses are only exposed when necessary.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "Email Exposure"))
                        .passed(false)
                        .recommendation("Review pages for unintentional data exposure. "
                                + "Use contact forms instead of publishing raw email addresses.")
                        .build());
            }

            if (CC_PATTERN.matcher(body).find()) {
                results.add(TestResult.builder("Sensitive Data: Credit Card Pattern Detected",
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.CRITICAL)
                        .endpoint(url)
                        .description("A string matching a credit card number pattern was found in the response body of "
                                + ep + ". PCI DSS compliance requires this data to never be stored or displayed.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "Credit Card Exposure"))
                        .passed(false)
                        .recommendation("Remove credit card data from HTTP responses immediately. "
                                + "Audit the data storage layer and comply with PCI DSS SAQ/ROC requirements.")
                        .build());
            }

            if (API_KEY_PATTERN.matcher(body).find()) {
                results.add(TestResult.builder("Sensitive Data: API Key / Secret Exposure",
                                OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.CRITICAL)
                        .endpoint(url)
                        .description("A string matching an API key or secret pattern was found in the response body of "
                                + ep + ". Rotate all leaked credentials immediately.")
                        .evidence(probe.toEvidenceString())
                        .screenshotPng(screenshots.capture(probe, "API Key Exposure"))
                        .passed(false)
                        .recommendation("Never expose API keys or secrets in HTTP responses. "
                                + "Store secrets in a vault (HashiCorp Vault, AWS Secrets Manager) "
                                + "and use environment variables at runtime.")
                        .build());
            }
        }
        return results;
    }

    private List<TestResult> testTlsProtocols(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        try {
            URI uri  = URI.create(baseUrl);
            String host = uri.getHost();
            int    port = uri.getPort() == -1 ? 443 : uri.getPort();

            // Attempt to negotiate TLS 1.0 / 1.1 (both deprecated by RFC 8996)
            for (String weakProto : List.of("TLSv1", "TLSv1.1")) {
                try {
                    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                        socket.setEnabledProtocols(new String[]{weakProto});
                        socket.startHandshake();
                        // Handshake succeeded → weak protocol accepted
                        results.add(TestResult.builder("Weak TLS Protocol Supported: " + weakProto,
                                        OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                                .severity(SeverityLevel.HIGH)
                                .endpoint(baseUrl)
                                .description("The server accepted a " + weakProto + " handshake. "
                                        + "This protocol is deprecated and has known vulnerabilities (POODLE, BEAST).")
                                .evidence("TLS handshake with " + weakProto + " succeeded against " + host + ":" + port)
                                .passed(false)
                                .recommendation("""
                                        Restrict TLS to 1.2+ in Apache2 SSL configuration:

                                        SSLProtocol -all +TLSv1.2 +TLSv1.3
                                        SSLCipherSuite ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384
                                        SSLHonorCipherOrder on""")
                                .build());
                    }
                } catch (Exception ignored) {
                    // Server refused → good
                }
            }

            if (results.isEmpty()) {
                results.add(TestResult.builder("TLS Protocol Versions", OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                        .severity(SeverityLevel.PASS)
                        .endpoint(baseUrl)
                        .description("Server refused TLSv1.0 and TLSv1.1 connections. Only TLS 1.2+ is accepted.")
                        .passed(true)
                        .build());
            }

        } catch (Exception e) {
            results.add(TestResult.builder("TLS Protocol Check – Error", OWASPCategory.A02_CRYPTOGRAPHIC_FAILURES)
                    .severity(SeverityLevel.INFO)
                    .endpoint(baseUrl)
                    .description("Could not perform TLS protocol check: " + e.getMessage())
                    .passed(true)
                    .build());
        }

        return results;
    }
}
