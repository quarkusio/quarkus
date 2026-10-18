package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class PermissionCheckerOnlySecurityIdentity {

    @PermissionChecker("only-security-identity")
    boolean isAdmin(SecurityIdentity securityIdentity) {
        return securityIdentity.hasRole("admin");
    }
}
