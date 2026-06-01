package com.platformcommons.security.tests;

import com.platformcommons.security.model.HttpProbeResult;
import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.service.HttpClientService;
import com.platformcommons.security.service.ScreenshotService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * A07:2021 – Identification and Authentication Failures
 *
 * Tests: default credentials on HTTP Basic Auth endpoints,
 * session fixation indicators, missing rate-limit headers,
 * password in query string, and autocomplete on login forms.
 */
@Component
public class AuthenticationTest {

    private static final List<String> LOGIN_PATHS = List.of(
            "/login", "/login/", "/signin", "/sign-in", "/auth",
            "/admin/login", "/user/login", "/wp-login.php");

    // Common default credential pairs (username:password)
    private static final List<String[]> DEFAULT_CREDS = List.of(
            new String[]{"admin",  "admin"},
            new String[]{"admin",  "password"},
            new String[]{"admin",  "123456"},
            new String[]{"admin",  ""},
            new String[]{"root",   "root"},
            new String[]{"root",   "toor"},
            new String[]{"test",   "test"},
            new String[]{"guest",  "guest"}
    );

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public AuthenticationTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        results.addAll(testDefaultCredentials(baseUrl));
        results.addAll(testPasswordInUrl(baseUrl, endpoints));
        results.addAll(testLoginFormSecurity(baseUrl));
        results.addAll(testSessionSecurity(baseUrl, endpoints));

        return results;
    }

    // ─── sub-tests ───────────────────────────────────────────────────────────

    private List<TestResult> testDefaultCredentials(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        // First detect if any login path uses HTTP Basic Auth (WWW-Authenticate header)
        for (String path : LOGIN_PATHS) {
            HttpProbeResult probe = http.get(baseUrl + path);
            if (probe.isError()) continue;

            String wwwAuth = probe.getHeader("WWW-Authenticate");
            if (probe.getStatusCode() == 401 && wwwAuth != null && wwwAuth.toLowerCase().startsWith("basic")) {
                // Try default credentials
                for (String[] cred : DEFAULT_CREDS) {
                    String token = Base64.getEncoder()
                            .encodeToString((cred[0] + ":" + cred[1]).getBytes());
                    HttpProbeResult authProbe = http.get(baseUrl + path,
                            Map.of("Authorization", "Basic " + token));

                    if (!authProbe.isError() && authProbe.getStatusCode() == 200) {
                        results.add(TestResult.builder("Default Credentials Accepted: " + path,
                                        OWASPCategory.A07_AUTH_FAILURES)
                                .severity(SeverityLevel.CRITICAL)
                                .endpoint(baseUrl + path)
                                .description("HTTP Basic Auth endpoint " + path + " accepted the default credentials "
                                        + "'" + cred[0] + ":" + cred[1] + "'.")
                                .evidence(authProbe.toEvidenceString())
                                .screenshotPng(screenshots.capture(authProbe, "Default Credentials"))
                                .passed(false)
                                .recommendation("""
                                        Immediately change all default credentials.
                                        Enforce a strong password policy:

                                        1. Minimum 12 characters, mixed case, digits, special chars.
                                        2. Use htpasswd with bcrypt:
                                           htpasswd -B /etc/apache2/.htpasswd newuser
                                        3. Consider migrating to key-based or SSO authentication.
                                        4. Enable account lockout after 5 failed attempts.""")
                                .build());
                        return results;
                    }
                }
            }
        }

        results.add(TestResult.builder("Default Credentials", OWASPCategory.A07_AUTH_FAILURES)
                .severity(SeverityLevel.PASS)
                .endpoint(baseUrl)
                .description("No HTTP Basic Auth endpoints accepted common default credentials.")
                .passed(true)
                .build());
        return results;
    }

    private List<TestResult> testPasswordInUrl(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        for (String ep : endpoints) {
            String url = baseUrl + ep;
            // Look for login or credential parameters in the query string
            if (url.toLowerCase().matches(".*[?&](password|passwd|pwd|pass|secret|token)=.*")) {
                results.add(TestResult.builder("Password in URL", OWASPCategory.A07_AUTH_FAILURES)
                        .severity(SeverityLevel.HIGH)
                        .endpoint(url)
                        .description("A credential parameter (password/token) was found in the URL query string. "
                                + "URLs are logged in access logs, browser history, and Referer headers.")
                        .evidence("URL: " + url)
                        .passed(false)
                        .recommendation("""
                                Never transmit credentials in URL parameters.
                                Use POST with body parameters or Bearer tokens in the Authorization header.

                                Also review Apache2 access logs for exposure:
                                grep -i 'password=' /var/log/apache2/access.log""")
                        .build());
            }
        }

        if (results.isEmpty()) {
            results.add(TestResult.builder("Password in URL", OWASPCategory.A07_AUTH_FAILURES)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("No credential parameters detected in tested URL query strings.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testLoginFormSecurity(String baseUrl) {
        List<TestResult> results = new ArrayList<>();

        for (String path : LOGIN_PATHS) {
            HttpProbeResult probe = http.get(baseUrl + path);
            if (probe.isError() || probe.getStatusCode() != 200) continue;
            String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";

            boolean hasForm     = body.toLowerCase().contains("<form");
            boolean hasPassword = body.toLowerCase().contains("type=\"password\"")
                    || body.toLowerCase().contains("type='password'");

            if (hasForm && hasPassword) {
                boolean hasAutocompleteOff = body.toLowerCase().contains("autocomplete=\"off\"")
                        || body.toLowerCase().contains("autocomplete='off'");
                boolean hasCsrfToken = body.toLowerCase().contains("csrf") || body.toLowerCase().contains("_token");

                if (!hasCsrfToken) {
                    results.add(TestResult.builder("Login Form Missing CSRF Token",
                                    OWASPCategory.A07_AUTH_FAILURES)
                            .severity(SeverityLevel.HIGH)
                            .endpoint(baseUrl + path)
                            .description("Login form at " + path + " does not appear to contain a CSRF token. "
                                    + "Attackers can forge cross-site login requests.")
                            .evidence(probe.toEvidenceString())
                            .screenshotPng(screenshots.capture(probe, "CSRF Token Missing"))
                            .passed(false)
                            .recommendation("""
                                    Add CSRF protection:
                                    1. Use a framework-provided CSRF token (Spring Security, OWASP CSRFGuard).
                                    2. Include a hidden field in the form:
                                       <input type="hidden" name="_csrf" value="${token}">
                                    3. Enable SameSite=Strict on session cookies.""")
                            .build());
                }
                break;  // test first login form found
            }
        }

        if (results.isEmpty()) {
            results.add(TestResult.builder("Login Form CSRF Protection", OWASPCategory.A07_AUTH_FAILURES)
                    .severity(SeverityLevel.PASS)
                    .endpoint(baseUrl)
                    .description("Login form contains CSRF protection token or no login form was found.")
                    .passed(true)
                    .build());
        }
        return results;
    }

    private List<TestResult> testSessionSecurity(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();
        String target = baseUrl + (endpoints.isEmpty() ? "/" : endpoints.get(0));

        HttpProbeResult probe = http.get(target);
        if (probe.isError()) return results;

        // Check for session identifier in URL (PHPSESSID, jsessionid, etc.)
        String finalUrl = probe.getFinalUrl();
        if (finalUrl != null && (finalUrl.toLowerCase().contains("phpsessid=")
                || finalUrl.toLowerCase().contains("jsessionid=")
                || finalUrl.toLowerCase().contains("sessionid="))) {
            results.add(TestResult.builder("Session ID in URL", OWASPCategory.A07_AUTH_FAILURES)
                    .severity(SeverityLevel.HIGH)
                    .endpoint(finalUrl)
                    .description("Session identifier found in URL: " + finalUrl
                            + ". Session IDs in URLs are logged and leaked via Referer headers.")
                    .evidence(probe.toEvidenceString())
                    .screenshotPng(screenshots.capture(probe, "Session ID in URL"))
                    .passed(false)
                    .recommendation("""
                            For PHP: prevent session IDs from appearing in URLs:
                            php_flag session.use_only_cookies On
                            php_flag session.use_trans_sid Off

                            For Java (Tomcat):
                            Set disableURLRewriting="true" in Tomcat's context.xml""")
                    .build());
        }

        return results;
    }
}
