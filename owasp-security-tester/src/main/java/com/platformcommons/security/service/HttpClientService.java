package com.platformcommons.security.service;

import com.platformcommons.security.model.HttpProbeResult;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin wrapper around Java's built-in HttpClient.
 * Ignores TLS certificate errors so self-signed / expired certs do not block tests.
 */
@Service
public class HttpClientService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final HttpClient noRedirectClient;

    public HttpClientService() {
        SSLContext sslContext = buildTrustAllSslContext();

        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.noRedirectClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Simple GET that follows redirects. */
    public HttpProbeResult get(String url) {
        return send(url, "GET", null, true, Map.of());
    }

    /** GET without following redirects. */
    public HttpProbeResult getNoFollow(String url) {
        return send(url, "GET", null, false, Map.of());
    }

    /** GET with extra request headers. */
    public HttpProbeResult get(String url, Map<String, String> extraHeaders) {
        return send(url, "GET", null, true, extraHeaders);
    }

    /** HEAD request. */
    public HttpProbeResult head(String url) {
        return send(url, "HEAD", null, true, Map.of());
    }

    /** Arbitrary method (OPTIONS, TRACE, DELETE …). */
    public HttpProbeResult method(String url, String method) {
        return send(url, method, null, true, Map.of());
    }

    // ─── internal ────────────────────────────────────────────────────────────

    private HttpProbeResult send(String url, String method, String body,
                                 boolean followRedirects, Map<String, String> extraHeaders) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT);

            extraHeaders.forEach(rb::header);
            rb.header("User-Agent", "OWASP-SecurityTester/1.0");

            if ("HEAD".equalsIgnoreCase(method)) {
                rb.method("HEAD", HttpRequest.BodyPublishers.noBody());
            } else if (body != null) {
                rb.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                rb.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpClient client = followRedirects ? httpClient : noRedirectClient;
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());

            Map<String, String> reqHeaders = new LinkedHashMap<>(extraHeaders);
            reqHeaders.put("User-Agent", "OWASP-SecurityTester/1.0");

            Map<String, String> respHeaders = new LinkedHashMap<>();
            resp.headers().map().forEach((k, vals) -> respHeaders.put(k, String.join(", ", vals)));

            boolean wasRedirected = !resp.uri().toString().equals(url);

            return HttpProbeResult.builder(url, method)
                    .statusCode(resp.statusCode())
                    .requestHeaders(reqHeaders)
                    .responseHeaders(respHeaders)
                    .responseBody(resp.body())
                    .elapsedMs(System.currentTimeMillis() - start)
                    .redirected(wasRedirected)
                    .finalUrl(resp.uri().toString())
                    .build();

        } catch (Exception ex) {
            return HttpProbeResult.builder(url, method)
                    .errorMessage(ex.getMessage())
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @SuppressWarnings("all")
    private static SSLContext buildTrustAllSslContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new java.security.SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build trust-all SSL context", e);
        }
    }
}
