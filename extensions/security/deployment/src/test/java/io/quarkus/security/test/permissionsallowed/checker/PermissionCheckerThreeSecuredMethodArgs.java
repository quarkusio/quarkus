package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class PermissionCheckerThreeSecuredMethodArgs {

    @PermissionChecker("three-args")
    boolean areThreeArgsOk(String three, SecurityIdentity securityIdentity, String one, Object two) {
        return Integer.parseInt(one) == 1 && Integer.parseInt(two.toString()) == 2 && Integer.parseInt(three) == 3
                && securityIdentity.hasRole("admin");
    }
}
