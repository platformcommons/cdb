package com.platformcommons.security.model;

import java.util.Map;

/**
 * Raw result of a single HTTP probe.
 */
public class HttpProbeResult {

    private final String  url;
    private final String  method;
    private final int     statusCode;
    private final Map<String, String> requestHeaders;
    private final Map<String, String> responseHeaders;
    private final String  responseBody;
    private final long    elapsedMs;
    private final String  errorMessage;   // non-null when the request itself failed
    private final boolean redirected;
    private final String  finalUrl;

    private HttpProbeResult(Builder b) {
        this.url             = b.url;
        this.method          = b.method;
        this.statusCode      = b.statusCode;
        this.requestHeaders  = b.requestHeaders;
        this.responseHeaders = b.responseHeaders;
        this.responseBody    = b.responseBody;
        this.elapsedMs       = b.elapsedMs;
        this.errorMessage    = b.errorMessage;
        this.redirected      = b.redirected;
        this.finalUrl        = b.finalUrl;
    }

    public boolean isError()      { return errorMessage != null; }
    public boolean isSuccess()    { return !isError() && statusCode >= 200 && statusCode < 300; }
    public boolean isRedirect()   { return !isError() && statusCode >= 300 && statusCode < 400; }

    public String  getUrl()              { return url; }
    public String  getMethod()           { return method; }
    public int     getStatusCode()       { return statusCode; }
    public Map<String, String> getRequestHeaders()  { return requestHeaders; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public String  getResponseBody()     { return responseBody; }
    public long    getElapsedMs()        { return elapsedMs; }
    public String  getErrorMessage()     { return errorMessage; }
    public boolean isRedirected()        { return redirected; }
    public String  getFinalUrl()         { return finalUrl; }

    public String getHeader(String name) {
        if (responseHeaders == null) return null;
        return responseHeaders.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /** Render as a human-readable request/response block for PDF evidence. */
    public String toEvidenceString() {
        StringBuilder sb = new StringBuilder();
        sb.append("REQUEST:\n");
        sb.append(method).append(" ").append(url).append("\n");
        if (requestHeaders != null) {
            requestHeaders.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        }
        sb.append("\nRESPONSE:\n");
        if (isError()) {
            sb.append("ERROR: ").append(errorMessage).append("\n");
        } else {
            sb.append("HTTP ").append(statusCode).append("\n");
            if (responseHeaders != null) {
                responseHeaders.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            }
            if (responseBody != null && !responseBody.isBlank()) {
                String preview = responseBody.length() > 500
                        ? responseBody.substring(0, 500) + "\n... [truncated]"
                        : responseBody;
                sb.append("\n").append(preview);
            }
        }
        return sb.toString();
    }

    // ─── builder ────────────────────────────────────────────────────────────

    public static Builder builder(String url, String method) {
        return new Builder(url, method);
    }

    public static class Builder {
        private final String url;
        private final String method;
        private int     statusCode;
        private Map<String, String> requestHeaders;
        private Map<String, String> responseHeaders;
        private String  responseBody;
        private long    elapsedMs;
        private String  errorMessage;
        private boolean redirected;
        private String  finalUrl;

        Builder(String url, String method) {
            this.url    = url;
            this.method = method;
            this.finalUrl = url;
        }

        public Builder statusCode(int v)                     { this.statusCode = v; return this; }
        public Builder requestHeaders(Map<String, String> v) { this.requestHeaders = v; return this; }
        public Builder responseHeaders(Map<String, String> v){ this.responseHeaders = v; return this; }
        public Builder responseBody(String v)                { this.responseBody = v; return this; }
        public Builder elapsedMs(long v)                     { this.elapsedMs = v; return this; }
        public Builder errorMessage(String v)                { this.errorMessage = v; return this; }
        public Builder redirected(boolean v)                 { this.redirected = v; return this; }
        public Builder finalUrl(String v)                    { this.finalUrl = v; return this; }

        public HttpProbeResult build() { return new HttpProbeResult(this); }
    }
}
