package com.platformcommons.security;

import com.platformcommons.security.model.TestResult;
import com.platformcommons.security.report.FixRecommendationGenerator;
import com.platformcommons.security.report.PdfReportGenerator;
import com.platformcommons.security.service.TestOrchestrator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@SpringBootApplication
public class OwaspTesterApplication {

    public static void main(String[] args) {
        // Suppress Spring Boot banner and startup logs for a cleaner CLI experience
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("logging.level.root", "ERROR");
        System.setProperty("java.awt.headless", "true");

        SpringApplication.run(OwaspTesterApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(TestOrchestrator orchestrator,
                                 PdfReportGenerator pdfGen,
                                 FixRecommendationGenerator fixGen) {
        return args -> {
            String       targetUrl  = null;
            List<String> endpoints  = new ArrayList<>();
            String       outputDir  = "./reports";

            for (String arg : args) {
                if (arg.startsWith("--url="))       targetUrl = arg.substring(6).trim();
                else if (arg.startsWith("--endpoints=")) {
                    String[] parts = arg.substring(12).split(",");
                    for (String p : parts) {
                        String ep = p.trim();
                        if (!ep.startsWith("/")) ep = "/" + ep;
                        endpoints.add(ep);
                    }
                } else if (arg.startsWith("--output=")) outputDir = arg.substring(9).trim();
                else if (arg.equals("--help") || arg.equals("-h")) {
                    printHelp();
                    return;
                }
            }

            if (targetUrl == null) {
                printHelp();
                System.exit(1);
            }

            // Default endpoint list
            if (endpoints.isEmpty()) endpoints = List.of("/");

            // Normalise base URL: strip trailing slash
            if (targetUrl.endsWith("/")) targetUrl = targetUrl.substring(0, targetUrl.length() - 1);

            printBanner(targetUrl, endpoints, outputDir);

            List<TestResult> results = orchestrator.runAllTests(targetUrl, endpoints);

            System.out.println("Generating PDF report…");
            String pdf = pdfGen.generateReport(results, targetUrl, outputDir);

            System.out.println("Generating remediation guide…");
            String txt = fixGen.generateGuide(results, targetUrl, outputDir);

            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("  OUTPUT FILES");
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("  PDF Report  : " + pdf);
            System.out.println("  Fix Guide   : " + txt);
            System.out.println("═══════════════════════════════════════════════════════");
        };
    }

    private void printBanner(String url, List<String> eps, String out) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║       OWASP TOP 10 SECURITY TESTER  v1.0.0          ║");
        System.out.println("║       Authorised testing only. Use responsibly.      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Target    : " + url);
        System.out.println("  Endpoints : " + eps);
        System.out.println("  Output    : " + out);
        System.out.println();
    }

    private void printHelp() {
        System.out.println("""
                OWASP Security Tester for Apache2
                Usage:
                  java -jar owasp-security-tester.jar \\
                       --url=<target-url>            \\
                       [--endpoints=<e1>,<e2>,...]   \\
                       [--output=<output-directory>]

                Arguments:
                  --url         Base URL of the Apache2 target (required)
                                Example: http://192.168.1.10
                  --endpoints   Comma-separated path list to test (default: /)
                                Example: /,/login,/admin,/api/users
                  --output      Directory for generated reports (default: ./reports)

                Outputs:
                  owasp_report_<timestamp>.pdf     — full PDF report with screenshots
                  REMEDIATION_GUIDE_<timestamp>.txt — Apache2 remediation steps

                IMPORTANT: Only use against systems you own or have written permission to test.
                """);
    }
}
