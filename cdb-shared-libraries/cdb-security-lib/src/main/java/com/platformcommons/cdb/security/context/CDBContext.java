package com.platformcommons.cdb.security.context;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Strongly-typed security context carried inside JWT (ctx claim).
 * Fields are optional and may be null depending on token type.
 */
public class CDBContext {

    private final UserContext user;
    private final ProviderContext provider;
    private final List<String> roles;
    private final List<String> authorities;

    // Raw claims for forward compatibility (non-type-safe extras)
    private final Map<String, Object> extras;

    private CDBContext(Builder b) {
        this.user = b.user;
        this.provider = b.provider;
        this.roles = b.roles == null ? List.of() : List.copyOf(b.roles);
        this.authorities = b.authorities == null ? List.of() : List.copyOf(b.authorities);
        this.extras = b.extras == null ? Map.of() : Collections.unmodifiableMap(b.extras);
    }

    public static Builder builder() {
        return new Builder();
    }

    public UserContext getUser() {
        return user;
    }

    public ProviderContext getProvider() {
        return provider;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public static class Builder {
        private UserContext user;
        private ProviderContext provider;
        private List<String> roles;
        private List<String> authorities;
        private Map<String, Object> extras;

        public Builder user(UserContext user) {
            this.user = user;
            return this;
        }

        public Builder provider(ProviderContext provider) {
            this.provider = provider;
            return this;
        }

        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public Builder authorities(List<String> authorities) {
            this.authorities = authorities;
            return this;
        }

        public Builder extras(Map<String, Object> extras) {
            this.extras = extras;
            return this;
        }

        public CDBContext build() {
            return new CDBContext(this);
        }
    }

    public static class UserContext {
        private final Long id;
        private final String login;

        public UserContext(Long id, String login) {
            this.id = id;
            this.login = login;
        }

        public Long getId() {
            return id;
        }

        public String getLogin() {
            return login;
        }
    }

    public static class ProviderContext {
        private final Long id;
        private final String code;

        public ProviderContext(Long id, String code) {
            this.id = id;
            this.code = code;
        }

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }
    }


}
