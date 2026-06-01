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
 * A10:2021 – Server-Side Request Forgery (SSRF)
 *
 * Probes URL parameters with internal addresses.
 * Detection is heuristic: we check whether cloud-metadata-like content is returned.
 */
@Component
public class SsrfTest {

    // Parameters commonly used to pass a URL
    private static final List<String> URL_PARAMS = List.of(
            "url", "uri", "link", "redirect", "next", "return", "callback",
            "file", "path", "src", "dest", "destination", "image", "proxy", "fetch");

    // Payloads pointing at cloud metadata / localhost
    private static final List<String> SSRF_PAYLOADS = List.of(
            "http://169.254.169.254/latest/meta-data/",          // AWS EC2
            "http://metadata.google.internal/computeMetadata/",  // GCP
            "http://169.254.169.254/metadata/instance",          // Azure IMDS
            "http://localhost/server-status",                     // Internal Apache status
            "http://127.0.0.1:8080/"                             // Internal app port
    );

    // Strings in response that indicate the SSRF hit something sensitive
    private static final List<String> SSRF_INDICATORS = List.of(
            "ami-id", "instance-id", "instance-type",
            "local-hostname", "computeMetadata", "tokenTTL",
            "availability-zone", "placement"
    );

    private final HttpClientService http;
    private final ScreenshotService screenshots;

    public SsrfTest(HttpClientService http, ScreenshotService screenshots) {
        this.http        = http;
        this.screenshots = screenshots;
    }

    public List<TestResult> run(String baseUrl, List<String> endpoints) {
        List<TestResult> results = new ArrayList<>();

        for (String ep : endpoints) {
            String endpointUrl = baseUrl + ep;
            results.addAll(probeSsrf(endpointUrl));
        }

        return results;
    }

    private List<TestResult> probeSsrf(String endpointUrl) {
        List<TestResult> results = new ArrayList<>();

        for (String param : URL_PARAMS) {
            for (String payload : SSRF_PAYLOADS) {
                String encoded = URLEncoder.encode(payload, StandardCharsets.UTF_8);
                String probeUrl = endpointUrl + (endpointUrl.contains("?") ? "&" : "?")
                        + param + "=" + encoded;

                HttpProbeResult probe = http.get(probeUrl);
                if (probe.isError()) continue;

                String body = probe.getResponseBody() != null ? probe.getResponseBody() : "";
                boolean hit  = SSRF_INDICATORS.stream().anyMatch(body::contains);

                if (hit) {
                    results.add(TestResult.builder("SSRF – Cloud Metadata Accessible",
                                    OWASPCategory.A10_SSRF)
                            .severity(SeverityLevel.CRITICAL)
                            .endpoint(probeUrl)
                            .description("SSRF probe using parameter '" + param + "' with payload '"
                                    + payload + "' returned cloud metadata content. "
                                    + "An attacker can read instance credentials and pivot to cloud resources.")
                            .evidence(probe.toEvidenceString())
                            .screenshotPng(screenshots.capture(probe, "SSRF"))
                            .passed(false)
                            .recommendation("""
                                    Mitigate SSRF:

                                    1. Validate and sanitise all URL inputs:
                                       - Allow only known safe domains (whitelist).
                                       - Block RFC-1918 ranges (10.x, 172.16-31.x, 192.168.x).
                                       - Block link-local (169.254.x.x) and loopback (127.x).

                                    2. In Apache2 proxy scenarios, restrict outbound with mod_proxy:
                                       ProxyRequests Off
                                       <Proxy *>
                                           Require all denied
                                       </Proxy>
                                       <Proxy "http://allowed-external-host.com/*">
                                           Require all granted
                                       </Proxy>

                                    3. On cloud instances, use IMDSv2 (AWS) which requires a session token –
                                       SSRF without the session step cannot retrieve metadata:
                                       aws ec2 modify-instance-metadata-options \\
                                           --http-tokens required \\
                                           --instance-id <id>""")
                            .build());
                    return results;
                }
            }
        }

        results.add(TestResult.builder("SSRF", OWASPCategory.A10_SSRF)
                .severity(SeverityLevel.PASS)
                .endpoint(endpointUrl)
                .description("SSRF probes against URL parameters did not return cloud metadata content. "
                        + "Note: absence of this indicator does not rule out SSRF; manual testing is advised.")
                .passed(true)
                .build());
        return results;
    }
}
