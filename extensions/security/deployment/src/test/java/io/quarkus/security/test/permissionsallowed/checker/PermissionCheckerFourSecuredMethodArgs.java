package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class PermissionCheckerFourSecuredMethodArgs {

    @PermissionChecker("four-args")
    public boolean isGranted(Object one, int two, String three, SecurityIdentity securityIdentity, Object four) {
        boolean methodArgsOk = Integer.parseInt(one.toString()) == 1 && two == 2 && Integer.parseInt(three) == 3
                && Integer.parseInt(four.toString()) == 4;
        return methodArgsOk && securityIdentity.hasRole("admin");
    }
}
