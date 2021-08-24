package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;

public class SupplierRolesAllowedCheck implements SecurityCheck {

    private final Supplier<String[]> allowedRolesSupplier;

    private volatile String[] allowedRoles;

    public SupplierRolesAllowedCheck(Supplier<String[]> allowedRolesSupplier) {
        this.allowedRolesSupplier = allowedRolesSupplier;
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        if (allowedRoles == null) {
            synchronized (this) {
                if (allowedRoles == null) {
                    allowedRoles = allowedRolesSupplier.get();
                }
            }
        }
        for (String role : allowedRoles) {
            if (identity.hasRole(role) || ("**".equals(role) && !identity.isAnonymous())) {
                return;
            }
        }
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }
}
