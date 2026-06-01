package com.platformcommons.security.model;

import java.time.LocalDateTime;

public class TestResult {

    private final String        testName;
    private final OWASPCategory category;
    private final SeverityLevel severity;
    private final String        description;
    private final String        evidence;
    private final boolean       passed;
    private final String        endpoint;
    private final String        recommendation;
    private final byte[]        screenshotPng;   // may be null
    private final LocalDateTime timestamp;

    private TestResult(Builder b) {
        this.testName       = b.testName;
        this.category       = b.category;
        this.severity       = b.severity;
        this.description    = b.description;
        this.evidence       = b.evidence;
        this.passed         = b.passed;
        this.endpoint       = b.endpoint;
        this.recommendation = b.recommendation;
        this.screenshotPng  = b.screenshotPng;
        this.timestamp      = LocalDateTime.now();
    }

    public String        getTestName()       { return testName; }
    public OWASPCategory getCategory()       { return category; }
    public SeverityLevel getSeverity()       { return severity; }
    public String        getDescription()    { return description; }
    public String        getEvidence()       { return evidence; }
    public boolean       isPassed()          { return passed; }
    public String        getEndpoint()       { return endpoint; }
    public String        getRecommendation() { return recommendation; }
    public byte[]        getScreenshotPng()  { return screenshotPng; }
    public LocalDateTime getTimestamp()      { return timestamp; }

    // ─── builder ────────────────────────────────────────────────────────────

    public static Builder builder(String testName, OWASPCategory category) {
        return new Builder(testName, category);
    }

    public static class Builder {
        private final String        testName;
        private final OWASPCategory category;
        private SeverityLevel severity       = SeverityLevel.INFO;
        private String        description    = "";
        private String        evidence       = "";
        private boolean       passed         = true;
        private String        endpoint       = "/";
        private String        recommendation = "";
        private byte[]        screenshotPng  = null;

        Builder(String testName, OWASPCategory category) {
            this.testName  = testName;
            this.category  = category;
        }

        public Builder severity(SeverityLevel v)       { this.severity       = v; return this; }
        public Builder description(String v)           { this.description    = v; return this; }
        public Builder evidence(String v)              { this.evidence       = v; return this; }
        public Builder passed(boolean v)               { this.passed         = v; return this; }
        public Builder endpoint(String v)              { this.endpoint       = v; return this; }
        public Builder recommendation(String v)        { this.recommendation = v; return this; }
        public Builder screenshotPng(byte[] v)         { this.screenshotPng  = v; return this; }

        public TestResult build() { return new TestResult(this); }
    }
}
