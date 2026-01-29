package com.platformcommons.cdb.security.auth;

import com.platformcommons.cdb.common.jpa.AuditContextProvider;
import org.springframework.stereotype.Component;

/**
 * Bridges security-lib's CDBSecurityUtil to the common-core auditing mechanism.
 */
@Component
public class SpringSecurityAuditContextProvider implements AuditContextProvider {

    private final CDBSecurityUtil securityUtil;

    public SpringSecurityAuditContextProvider(CDBSecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @Override
    public Long getCurrentUserId() {
        try {
            return securityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long getCurrentProviderId() {
        try {
            return securityUtil.getCurrentProviderId();
        } catch (Exception e) {
            return null;
        }
    }
}
