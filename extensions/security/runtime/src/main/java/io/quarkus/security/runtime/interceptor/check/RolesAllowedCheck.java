package io.quarkus.security.runtime.interceptor.check;

import java.util.Set;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

public class RolesAllowedCheck implements SecurityCheck {
    private final String[] allowedRoles;

    public RolesAllowedCheck(String[] allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    @Override
    public void apply(SecurityIdentity identity) {
        Set<String> roles = identity.getRoles();
        if (roles != null) {
            for (String role : allowedRoles) {
                if (roles.contains(role)) {
                    return;
                }
            }
        }
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }
}
