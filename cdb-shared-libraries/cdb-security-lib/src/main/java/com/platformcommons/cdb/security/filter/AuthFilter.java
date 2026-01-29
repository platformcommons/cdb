package com.platformcommons.cdb.security.filter;

import com.platformcommons.cdb.security.auth.CDBContextAuthentication;
import com.platformcommons.cdb.security.context.CDBContext;
import com.platformcommons.cdb.security.jwt.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * AuthFilter validates Bearer JWT on protected endpoints.
 * - Skips known public endpoints (OpenAPI/Swagger and some public auth/provider endpoints).
 * - If a protected path has no/invalid/expired token, responds with 401.
 */
public class AuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public AuthFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (PublicEndpoints.isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Bearer token");
            return;
        }
        String token = auth.substring(7);
        try {
            if (!jwtTokenService.validate(token)) {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
            // Set Authentication so downstream can see authenticated user
            Claims claims = jwtTokenService.parseClaims(token);
            String username = claims.getSubject();

            // Extract context map from claim key "ctx" (backward-compatible if absent)
            Object ctxObj = claims.get("ctx");
            Map<String, Object> ctx = ctxObj instanceof Map ? (Map<String, Object>) ctxObj : Map.of();

            // Build authorities from ctx.roles and ctx.authorities if present
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = new ArrayList<>();
            List<String> authz = new ArrayList<>();
            try {
                Object rolesObj = ctx.get("roles");
                if (rolesObj instanceof Iterable<?> rolesIt) {
                    for (Object r : rolesIt) {
                        if (r != null) {
                            String rc = r.toString();
                            roles.add(rc);
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + rc));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                Object authObj = ctx.get("authorities");
                if (authObj instanceof Iterable<?> authIt) {
                    for (Object a : authIt) {
                        if (a != null) {
                            String ac = a.toString();
                            authz.add(ac);
                            authorities.add(new SimpleGrantedAuthority(ac));
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // Build type-safe context
            CDBContext.UserContext userCtx = null;
            try {
                Object uo = ctx.get("user");
                if (uo instanceof Map<?, ?> um) {
                    Long id = null;
                    Object idObj = um.get("id");
                    if (idObj instanceof Number n) id = n.longValue();
                    else if (idObj != null) {
                        try {
                            id = Long.parseLong(idObj.toString());
                        } catch (Exception ignored2) {
                        }
                    }
                    String login = um.get("login") == null ? null : um.get("login").toString();
                    userCtx = new CDBContext.UserContext(id, login);
                }
            } catch (Exception ignored) {
            }

            CDBContext.ProviderContext providerCtx = null;
            try {
                Object po = ctx.get("provider");
                if (po instanceof Map<?, ?> pm) {
                    Long id = null;
                    Object idObj = pm.get("id");
                    if (idObj instanceof Number n) id = n.longValue();
                    else if (idObj != null) {
                        try {
                            id = Long.parseLong(idObj.toString());
                        } catch (Exception ignored2) {
                        }
                    }
                    String code = pm.get("code") == null ? null : pm.get("code").toString();
                    providerCtx = new CDBContext.ProviderContext(id, code);
                }
            } catch (Exception ignored) {
            }

            CDBContext context = CDBContext.builder()
                    .user(userCtx)
                    .provider(providerCtx)
                    .roles(roles)
                    .authorities(authz)
                    .extras(ctx)
                    .build();

            CDBContextAuthentication authentication = new CDBContextAuthentication(username, context,
                    authorities,token);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
