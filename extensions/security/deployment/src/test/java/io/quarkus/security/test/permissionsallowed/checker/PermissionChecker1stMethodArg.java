package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class PermissionChecker1stMethodArg extends AbstractNthMethodArgChecker {

    @PermissionChecker("1st-arg")
    boolean is1stMethodArgOk(Object one, SecurityIdentity identity) {
        return this.argsOk(1, one, identity);
    }

}
