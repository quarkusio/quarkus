package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class PermissionChecker4thMethodArg extends AbstractNthMethodArgChecker {

    @Inject
    SecurityIdentity identity;

    @PermissionChecker("4th-arg")
    boolean is4thMethodArgOk(Object four) {
        return this.argsOk(4, four, identity);
    }
}
