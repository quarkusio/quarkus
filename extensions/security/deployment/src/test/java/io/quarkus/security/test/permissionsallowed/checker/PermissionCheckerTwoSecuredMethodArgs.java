package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public class PermissionCheckerTwoSecuredMethodArgs {

    @PermissionChecker("two-args")
    boolean areTwoArgsOk(long two, long one, SecurityIdentity securityIdentity) {
        return one == 1 && two == 2 && securityIdentity.hasRole("admin");
    }
}
