package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public class PermissionChecker6thMethodArg extends AbstractNthMethodArgChecker {

    @Inject
    SecurityIdentity identity;

    @PermissionChecker("6th-arg")
    boolean is6thMethodArgOk(Object six) {
        return this.argsOk(6, six, identity);
    }

    @PermissionChecker("another-6th-arg")
    boolean is6thMethodArgOk_Another(Object six) {
        if (six instanceof Long) {
            return false;
        }
        return this.argsOk(6, six, identity);
    }
}
