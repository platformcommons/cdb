package com.platformcommons.cdb.security.auth;

import com.platformcommons.cdb.security.context.CDBContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CDBSecurityUtil {

    public Long getCurrentProviderId() {
        return getCurrentProviderContext().getId();
    }

    public String getCurrentProviderCode() {
        return getCurrentProviderContext().getCode();
    }

    public Long getCurrentUserId() {
        return getCurrentUserContext().getId();
    }

    public String getCurrentUserLogin() {
        return getCurrentUserContext().getLogin();
    }

    public String  getCurrentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CDBContextAuthentication cdbAuth) {
            return cdbAuth.getAccessToken();
        }
        throw new IllegalStateException("Invalid authentication context");
    }

    private CDBContext.ProviderContext getCurrentProviderContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CDBContextAuthentication cdbAuth) {
            return cdbAuth.getContext().getProvider();
        }
        throw new IllegalStateException("Invalid authentication context");
    }


    private CDBContext.UserContext getCurrentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CDBContextAuthentication cdbAuth) {
            return cdbAuth.getContext().getUser();
        }
        throw new IllegalStateException("Invalid authentication context");
    }


}
