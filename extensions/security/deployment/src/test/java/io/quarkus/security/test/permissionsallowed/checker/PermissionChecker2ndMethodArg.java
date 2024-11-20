package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public class PermissionChecker2ndMethodArg extends AbstractNthMethodArgChecker {

    @PermissionChecker("2nd-arg")
    boolean is2ndMethodArgOk(Object two, SecurityIdentity identity) {
        return this.argsOk(2, two, identity);
    }

}
