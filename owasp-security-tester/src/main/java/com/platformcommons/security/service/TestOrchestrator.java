package com.platformcommons.security.service;

import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.tests.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates all OWASP test modules and collects results.
 */
@Service
public class TestOrchestrator {

    private final SecurityHeadersTest     securityHeaders;
    private final BrokenAccessControlTest accessControl;
    private final CryptographicFailuresTest cryptographic;
    private final InjectionTest           injection;
    private final MisconfigurationTest    misconfiguration;
    private final AuthenticationTest      authentication;
    private final VulnerableComponentsTest vulnerableComponents;
    private final LoggingMonitoringTest   loggingMonitoring;
    private final SsrfTest                ssrf;

    public TestOrchestrator(
            SecurityHeadersTest     securityHeaders,
            BrokenAccessControlTest accessControl,
            CryptographicFailuresTest cryptographic,
            InjectionTest           injection,
            MisconfigurationTest    misconfiguration,
            AuthenticationTest      authentication,
            VulnerableComponentsTest vulnerableComponents,
            LoggingMonitoringTest   loggingMonitoring,
            SsrfTest                ssrf) {

        this.securityHeaders      = securityHeaders;
        this.accessControl        = accessControl;
        this.cryptographic        = cryptographic;
        this.injection            = injection;
        this.misconfiguration     = misconfiguration;
        this.authentication       = authentication;
        this.vulnerableComponents = vulnerableComponents;
        this.loggingMonitoring    = loggingMonitoring;
        this.ssrf                 = ssrf;
    }

    public List<TestResult> runAllTests(String baseUrl, List<String> endpoints) {
        List<TestResult> all = new ArrayList<>();

        System.out.println("\n[1/9] Running Security Headers checks (A05, A02)...");
        runModule("Security Headers",     () -> securityHeaders.run(baseUrl, endpoints),     all);

        System.out.println("[2/9] Running Broken Access Control checks (A01)...");
        runModule("Broken Access Control", () -> accessControl.run(baseUrl, endpoints),       all);

        System.out.println("[3/9] Running Cryptographic Failures checks (A02)...");
        runModule("Cryptographic Failures", () -> cryptographic.run(baseUrl, endpoints),      all);

        System.out.println("[4/9] Running Injection checks (A03)...");
        runModule("Injection",             () -> injection.run(baseUrl, endpoints),            all);

        System.out.println("[5/9] Running Security Misconfiguration checks (A05)...");
        runModule("Misconfiguration",      () -> misconfiguration.run(baseUrl, endpoints),     all);

        System.out.println("[6/9] Running Authentication Failures checks (A07)...");
        runModule("Authentication",        () -> authentication.run(baseUrl, endpoints),       all);

        System.out.println("[7/9] Running Vulnerable Components checks (A06)...");
        runModule("Vulnerable Components", () -> vulnerableComponents.run(baseUrl, endpoints), all);

        System.out.println("[8/9] Running Logging & Monitoring checks (A09)...");
        runModule("Logging & Monitoring",  () -> loggingMonitoring.run(baseUrl, endpoints),    all);

        System.out.println("[9/9] Running SSRF checks (A10)...");
        runModule("SSRF",                  () -> ssrf.run(baseUrl, endpoints),                 all);

        // Sort: failures first, then by severity weight desc, then by category
        all.sort(Comparator
                .comparingInt((TestResult r) -> r.isPassed() ? 1 : 0)
                .thenComparingInt(r -> -r.getSeverity().getWeight())
                .thenComparing(r -> r.getCategory().getId()));

        printSummary(all);
        return all;
    }

    private void runModule(String name, java.util.function.Supplier<List<TestResult>> supplier,
                           List<TestResult> collector) {
        try {
            List<TestResult> results = supplier.get();
            collector.addAll(results);
            long issues = results.stream().filter(r -> !r.isPassed()).count();
            System.out.printf("    → %d test(s), %d issue(s)%n", results.size(), issues);
        } catch (Exception e) {
            System.err.println("    ✗ " + name + " module failed: " + e.getMessage());
        }
    }

    private void printSummary(List<TestResult> all) {
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  SCAN SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.printf("  Total checks : %d%n", all.size());
        System.out.printf("  ✓ Passed     : %d%n", all.stream().filter(TestResult::isPassed).count());
        System.out.printf("  ✗ Findings   : %d%n", all.stream().filter(r -> !r.isPassed()).count());
        System.out.println();

        for (SeverityLevel sev : List.of(SeverityLevel.CRITICAL, SeverityLevel.HIGH,
                SeverityLevel.MEDIUM, SeverityLevel.LOW, SeverityLevel.INFO)) {
            long count = all.stream()
                    .filter(r -> !r.isPassed() && r.getSeverity() == sev)
                    .count();
            if (count > 0) System.out.printf("  %-12s : %d%n", sev.getLabel(), count);
        }
        System.out.println("═══════════════════════════════════════════════════════\n");
    }
}
