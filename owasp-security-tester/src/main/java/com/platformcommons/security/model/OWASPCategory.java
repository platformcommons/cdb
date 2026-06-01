package com.platformcommons.security.model;

public enum OWASPCategory {
    A01_BROKEN_ACCESS_CONTROL("A01:2021", "Broken Access Control",
            "Access control enforces policy such that users cannot act outside of their intended permissions."),
    A02_CRYPTOGRAPHIC_FAILURES("A02:2021", "Cryptographic Failures",
            "Failures related to cryptography that often lead to sensitive data exposure."),
    A03_INJECTION("A03:2021", "Injection",
            "Hostile data sent to an interpreter as part of a command or query."),
    A04_INSECURE_DESIGN("A04:2021", "Insecure Design",
            "Missing or ineffective control designs exposing risks."),
    A05_SECURITY_MISCONFIGURATION("A05:2021", "Security Misconfiguration",
            "Improperly configured permissions, unnecessary features, default accounts."),
    A06_VULNERABLE_COMPONENTS("A06:2021", "Vulnerable and Outdated Components",
            "Using components with known vulnerabilities without proper patch management."),
    A07_AUTH_FAILURES("A07:2021", "Identification and Authentication Failures",
            "Incorrect implementation of authentication allowing attackers to assume other users' identities."),
    A08_INTEGRITY_FAILURES("A08:2021", "Software and Data Integrity Failures",
            "Infrastructure and code that does not protect against integrity violations."),
    A09_LOGGING_FAILURES("A09:2021", "Security Logging and Monitoring Failures",
            "Insufficient logging and monitoring allowing breaches to go undetected."),
    A10_SSRF("A10:2021", "Server-Side Request Forgery",
            "Forcing the server-side application to make requests to an unintended location.");

    private final String id;
    private final String name;
    private final String description;

    OWASPCategory(String id, String name, String description) {
        this.id          = id;
        this.name        = name;
        this.description = description;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }

    public String getFullTitle() { return id + " – " + name; }
}
