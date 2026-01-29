package com.platformcommons.cdb.platform.api.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that performs request transformations (headers, path, body) before routing.
 * <p>
 * The implementation here is intentionally minimal and non-destructive, serving as a
 * placeholder for real transformation rules (e.g., header injections, path rewrites).
 * </p>
 */
@Component
@Order(30)
public class RequestTransformationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Placeholder: Add or adjust headers for downstream services.
        // Example: propagate a correlation id if absent.
        if (request.getHeader("X-Correlation-Id") == null) {
            response.addHeader("X-Correlation-Id", java.util.UUID.randomUUID().toString());
        }
        filterChain.doFilter(request, response);
    }
}
