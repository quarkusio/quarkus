package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class SecurityIdentityOnlyPermissionChecker {

    @PermissionChecker("security-identity-only")
    public boolean hasAdminPrincipalName(SecurityIdentity securityIdentity) {
        return !securityIdentity.isAnonymous() && "admin".equals(securityIdentity.getPrincipal().getName());
    }

}
