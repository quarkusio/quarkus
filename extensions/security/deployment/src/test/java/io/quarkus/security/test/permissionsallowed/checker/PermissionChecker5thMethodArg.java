package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public class PermissionChecker5thMethodArg extends AbstractNthMethodArgChecker {

    @PermissionChecker("5th-arg")
    boolean is5thMethodArgOk(SecurityIdentity identity, Object five) {
        return this.argsOk(5, five, identity);
    }
}
