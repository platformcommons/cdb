package com.platformcommons.cdb.security.auth;

import com.platformcommons.cdb.security.context.CDBContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication object carrying a type-safe context parsed from JWT claims.
 * This replaces the default UsernamePasswordAuthenticationToken for requests authenticated via JWT.
 */
public class CDBContextAuthentication extends AbstractAuthenticationToken {

    private final Object principal;
    private final CDBContext context;
    private final String accessToken;

    public CDBContextAuthentication(Object principal,
                                    CDBContext context,
                                    Collection<? extends GrantedAuthority> authorities, String accessToken) {
        super(authorities);
        this.principal = principal;
        this.context = context;
        this.accessToken = accessToken;
        // JWT is already validated by filter
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // credentials are not held post-authentication
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public CDBContext getContext() {
        return context;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
