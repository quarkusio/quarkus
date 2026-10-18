package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class PermissionChecker3rdMethodArg extends AbstractNthMethodArgChecker {

    @PermissionChecker("3rd-arg")
    boolean is3rdMethodArgOk(Object three, SecurityIdentity identity) {
        return this.argsOk(3, three, identity);
    }
}
