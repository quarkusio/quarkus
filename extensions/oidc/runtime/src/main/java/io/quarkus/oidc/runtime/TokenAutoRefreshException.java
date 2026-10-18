package io.quarkus.oidc.runtime;

import io.quarkus.security.identity.SecurityIdentity;

@SuppressWarnings("serial")
public class TokenAutoRefreshException extends RuntimeException {
    private SecurityIdentity securityIdentity;

    public TokenAutoRefreshException(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
