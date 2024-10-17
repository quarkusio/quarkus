package io.quarkus.security.test.permissionsallowed.checker;

import io.quarkus.security.identity.SecurityIdentity;

abstract class AbstractNthMethodArgChecker {

    protected boolean argsOk(int expected, Object arg, SecurityIdentity identity) {
        return Integer.parseInt(arg.toString()) == expected && identity.hasRole("admin");
    }

}
