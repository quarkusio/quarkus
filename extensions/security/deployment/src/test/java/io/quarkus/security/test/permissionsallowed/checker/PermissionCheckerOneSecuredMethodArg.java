package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public class PermissionCheckerOneSecuredMethodArg {

    @PermissionChecker("one-arg")
    public boolean isGranted(SecurityIdentity securityIdentity, Object one) {
        return Integer.parseInt(one.toString()) == 1 && securityIdentity.hasRole("admin");
    }
}
