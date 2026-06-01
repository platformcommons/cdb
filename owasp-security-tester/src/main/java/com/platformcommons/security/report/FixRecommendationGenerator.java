package com.platformcommons.security.report;

import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a human-readable plain-text remediation guide.
 *
 * The output is a standalone TXT file containing Apache2-specific
 * configuration snippets and application-level fixes for every
 * finding raised by the OWASP test suite.
 */
@Component
public class FixRecommendationGenerator {

    private static final String LINE = "═".repeat(72);
    private static final String DASH = "─".repeat(72);
    private static final String SEP  = "-".repeat(72);

    public String generateGuide(List<TestResult> results, String targetUrl, String outputDir) throws Exception {
        new File(outputDir).mkdirs();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filePath  = outputDir + File.separator + "REMEDIATION_GUIDE_" + timestamp + ".txt";

        List<TestResult> issues = results.stream()
                .filter(r -> !r.isPassed())
                .sorted(Comparator.comparingInt((TestResult r) -> -r.getSeverity().getWeight())
                        .thenComparing(r -> r.getCategory().getId()))
                .collect(Collectors.toList());

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            writeHeader(pw, targetUrl, issues);
            writeQuickWins(pw, issues);
            writeFindingsByPriority(pw, issues);
            writeApacheHardeningTemplate(pw);
            writeReferences(pw);
        }

        return filePath;
    }

    // ─── sections ─────────────────────────────────────────────────────────────

    private void writeHeader(PrintWriter pw, String targetUrl, List<TestResult> issues) {
        pw.println(LINE);
        pw.println("  OWASP TOP 10 REMEDIATION GUIDE");
        pw.println("  Apache2 Security Hardening");
        pw.println(LINE);
        pw.println();
        pw.println("  Target URL   : " + targetUrl);
        pw.println("  Generated    : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")));
        pw.println("  Total Issues : " + issues.size());
        pw.println();

        pw.println("  SEVERITY BREAKDOWN");
        pw.println(DASH);
        for (SeverityLevel sev : List.of(SeverityLevel.CRITICAL, SeverityLevel.HIGH,
                SeverityLevel.MEDIUM, SeverityLevel.LOW, SeverityLevel.INFO)) {
            long cnt = issues.stream().filter(r -> r.getSeverity() == sev).count();
            if (cnt > 0) pw.printf("  %-12s : %d issue(s)%n", sev.getLabel(), cnt);
        }

        pw.println();
        pw.println("  NOTE: This guide provides technical remediation steps for Apache2.");
        pw.println("  Application-level fixes may also be required. Always test changes");
        pw.println("  in a staging environment before applying to production.");
        pw.println();
        pw.println(LINE);
        pw.println();
    }

    private void writeQuickWins(PrintWriter pw, List<TestResult> issues) {
        pw.println("QUICK WINS  (apply these first – low effort, high impact)");
        pw.println(DASH);
        pw.println();

        // The most impactful one-liners
        pw.println("  1. Suppress version info in /etc/apache2/conf-available/security.conf :");
        pw.println("        ServerTokens Prod");
        pw.println("        ServerSignature Off");
        pw.println("        TraceEnable Off");
        pw.println();

        pw.println("  2. Enable mod_headers and add security headers:");
        pw.println("        sudo a2enmod headers");
        pw.println("        sudo a2enmod rewrite");
        pw.println("        sudo systemctl restart apache2");
        pw.println();

        pw.println("  3. Disable directory listing globally:");
        pw.println("        <Directory /var/www/html>");
        pw.println("            Options -Indexes -FollowSymLinks");
        pw.println("        </Directory>");
        pw.println();

        pw.println("  4. Disable TRACE method:");
        pw.println("        TraceEnable Off");
        pw.println();

        pw.println(LINE);
        pw.println();
    }

    private void writeFindingsByPriority(PrintWriter pw, List<TestResult> issues) {
        pw.println("DETAILED FINDINGS & REMEDIATION STEPS");
        pw.println(LINE);
        pw.println();

        // Group by severity then write each finding
        Map<SeverityLevel, List<TestResult>> grouped = issues.stream()
                .collect(Collectors.groupingBy(TestResult::getSeverity,
                        LinkedHashMap::new, Collectors.toList()));

        int findingNum = 1;
        for (SeverityLevel sev : List.of(SeverityLevel.CRITICAL, SeverityLevel.HIGH,
                SeverityLevel.MEDIUM, SeverityLevel.LOW, SeverityLevel.INFO)) {
            List<TestResult> group = grouped.get(sev);
            if (group == null || group.isEmpty()) continue;

            pw.println("━━ " + sev.getLabel().toUpperCase() + " SEVERITY (" + group.size() + ") ━━");
            pw.println();

            for (TestResult r : group) {
                writeFinding(pw, r, findingNum++);
            }
        }
    }

    private void writeFinding(PrintWriter pw, TestResult r, int num) {
        pw.println(SEP);
        pw.printf("  [%d] %s%n", num, r.getTestName());
        pw.println(SEP);
        pw.println();
        pw.println("  Category    : " + r.getCategory().getFullTitle());
        pw.println("  Severity    : " + r.getSeverity().getLabel());
        pw.println("  Endpoint    : " + r.getEndpoint());
        pw.println();

        pw.println("  DESCRIPTION");
        pw.println("  " + SEP);
        printWrapped(pw, r.getDescription(), "  ");
        pw.println();

        if (r.getRecommendation() != null && !r.getRecommendation().isBlank()) {
            pw.println("  REMEDIATION");
            pw.println("  " + SEP);
            // Print recommendation with consistent indentation
            for (String line : r.getRecommendation().split("\n")) {
                pw.println("  " + line);
            }
            pw.println();
        }

        // Append OWASP reference
        pw.println("  OWASP Reference : https://owasp.org/Top10/A0"
                + r.getCategory().getId().replaceAll("[^0-9]", "").substring(0, 1)
                + "_2021-" + r.getCategory().getName().replaceAll("[^a-zA-Z0-9]", "_") + "/");
        pw.println();
    }

    private void writeApacheHardeningTemplate(PrintWriter pw) {
        pw.println();
        pw.println(LINE);
        pw.println("APACHE2 FULL HARDENING TEMPLATE");
        pw.println("Save as: /etc/apache2/conf-available/hardening.conf");
        pw.println("Enable with: sudo a2enconf hardening && sudo systemctl reload apache2");
        pw.println(LINE);
        pw.println();
        pw.println("""
# ─────────────────────────────────────────────────────────────────
# Apache2 Security Hardening Configuration
# Generated by OWASP Security Tester
# Apply after testing in staging environment.
# ─────────────────────────────────────────────────────────────────

# ── Version disclosure ────────────────────────────────────────────
ServerTokens Prod
ServerSignature Off
TraceEnable Off

# ── Security Headers (requires mod_headers) ───────────────────────
<IfModule mod_headers.c>
    # Prevent clickjacking
    Header always set X-Frame-Options "SAMEORIGIN"

    # Prevent MIME type sniffing
    Header always set X-Content-Type-Options "nosniff"

    # Enable XSS filter (legacy browsers)
    Header always set X-XSS-Protection "1; mode=block"

    # Referrer policy
    Header always set Referrer-Policy "strict-origin-when-cross-origin"

    # Permissions policy – disable unused browser features
    Header always set Permissions-Policy "camera=(), microphone=(), geolocation=(), interest-cohort=()"

    # Content Security Policy – tighten for your application
    Header always set Content-Security-Policy "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'self'"

    # HTTP Strict Transport Security (enable ONLY on HTTPS vhosts)
    # Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"

    # Remove tech stack headers
    Header unset X-Powered-By
    Header always unset X-Powered-By
</IfModule>

# ── Directory hardening ───────────────────────────────────────────
<Directory />
    Options None
    AllowOverride None
    Require all denied
</Directory>

<Directory /var/www/html>
    Options -Indexes -FollowSymLinks -MultiViews
    AllowOverride None
    Require all granted
</Directory>

# ── Deny sensitive files ──────────────────────────────────────────
<FilesMatch "^\\.ht|^\\.git|^\\.env|\\.sql$|\\.bak$|Dockerfile|composer\\.json|package\\.json">
    Require all denied
</FilesMatch>

<DirectoryMatch "/\\.git">
    Require all denied
</DirectoryMatch>

# ── Restrict HTTP methods ─────────────────────────────────────────
<LimitExcept GET POST HEAD>
    Require all denied
</LimitExcept>

# ── Restrict server-status / server-info ─────────────────────────
<Location /server-status>
    Require local
</Location>
<Location /server-info>
    Require local
</Location>

# ── Custom error pages (create /var/www/html/errors/*.html) ───────
ErrorDocument 400 /errors/400.html
ErrorDocument 403 /errors/403.html
ErrorDocument 404 /errors/404.html
ErrorDocument 500 /errors/500.html

# ── HTTPS redirect (place in *:80 VirtualHost) ───────────────────
# <VirtualHost *:80>
#     RewriteEngine On
#     RewriteCond %{HTTPS} off
#     RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
# </VirtualHost>

# ── SSL / TLS hardening (place in *:443 VirtualHost) ─────────────
# SSLProtocol -all +TLSv1.2 +TLSv1.3
# SSLCipherSuite ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305
# SSLHonorCipherOrder on
# SSLCompression off
# SSLSessionTickets off
""");
        pw.println(LINE);
        pw.println();
    }

    private void writeReferences(PrintWriter pw) {
        pw.println("USEFUL REFERENCES");
        pw.println(DASH);
        pw.println();
        pw.println("  OWASP Top 10:2021");
        pw.println("  https://owasp.org/Top10/");
        pw.println();
        pw.println("  OWASP Apache Cheat Sheet");
        pw.println("  https://cheatsheetseries.owasp.org/cheatsheets/DotNet_Security_Cheat_Sheet.html");
        pw.println();
        pw.println("  Mozilla Security Headers");
        pw.println("  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers#security");
        pw.println();
        pw.println("  Apache2 Security Tips");
        pw.println("  https://httpd.apache.org/docs/2.4/misc/security_tips.html");
        pw.println();
        pw.println("  CIS Apache HTTP Server Benchmark");
        pw.println("  https://www.cisecurity.org/benchmark/apache_http_server");
        pw.println();
        pw.println("  TLS Best Practices");
        pw.println("  https://wiki.mozilla.org/Security/Server_Side_TLS");
        pw.println();
        pw.println(LINE);
        pw.println("  END OF REMEDIATION GUIDE");
        pw.println(LINE);
    }

    // ─── util ─────────────────────────────────────────────────────────────────

    private void printWrapped(PrintWriter pw, String text, String indent) {
        if (text == null) return;
        for (String line : text.split("\n")) {
            // Simple word wrap at 72 chars
            while (line.length() > 70) {
                int cut = line.lastIndexOf(' ', 70);
                if (cut == -1) cut = 70;
                pw.println(indent + line.substring(0, cut));
                line = line.substring(cut).stripLeading();
            }
            pw.println(indent + line);
        }
    }
}
