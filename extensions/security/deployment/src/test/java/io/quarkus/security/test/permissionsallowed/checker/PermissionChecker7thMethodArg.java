package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public class PermissionChecker7thMethodArg extends AbstractNthMethodArgChecker {

    @PermissionChecker("7th-arg")
    boolean is7thMethodArgOk(Object seven, SecurityIdentity securityIdentity) {
        return this.argsOk(7, seven, securityIdentity);
    }

    @PermissionChecker("another-7th-arg")
    boolean is7thMethodArgOk_Another(SecurityIdentity securityIdentity, Object seven) {
        if (seven instanceof String) {
            return false;
        }
        return this.argsOk(7, seven, securityIdentity);
    }
}
