package com.platformcommons.cdb.security.filter;

/**
 * Centralized constants and helpers for public (unauthenticated) endpoints.
 * Can be referenced from filters and security config to keep behavior aligned.
 */
public final class PublicEndpoints {

    // List of public path patterns (prefix-based for entries ending with '/').
    public static final String[] PUBLIC_PATTERNS = new String[]{
            "/index.html", "/login", "/signup", "/signup/","/login/","/app/",
            "/static/", "/assets/", "/favicon.ico", "/manifest.webmanifest",
            "/v3/api-docs/", "/swagger-ui.html", "/swagger-ui/",

            // Public authentication and user endpoints
            "/api/v1/providers/register/precheck", "/api/v1/providers/register",
            "/api/v1/users/exists", "/api/v1/users/register", "/api/v1/auth/login",
            "/api/v1/otp/**",
            "/api/v1/api-registry/discovery/**",
            // JWKS endpoints must be public for verifiers to fetch signing keys
            "/.well-known/jwks.json", "/jwks.json",
            "/oauth2/**",
            "/error"};

    private PublicEndpoints() {
    }

    public static boolean isPublicPath(String path) {
        if (path == null) return true;
        for (String p : PUBLIC_PATTERNS) {
            if (p.endsWith("/**") || p.endsWith("/*")) {
                String base = p.replace("/**", "/").replace("/*", "/");
                if (path.startsWith(base)) return true;
            }
            if (p.endsWith("/")) {
                if (path.startsWith(p)) return true;
            }
            if (path.equals(p)) return true;
        }
        return false;
    }
}
