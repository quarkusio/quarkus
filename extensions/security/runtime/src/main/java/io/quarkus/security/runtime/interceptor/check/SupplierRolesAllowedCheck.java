package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;

public class SupplierRolesAllowedCheck implements SecurityCheck {

    private final Supplier<String[]> allowedRolesSupplier;

    private volatile String[] allowedRoles;

    public SupplierRolesAllowedCheck(Supplier<String[]> allowedRolesSupplier) {
        this.allowedRolesSupplier = allowedRolesSupplier;
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        doApply(identity);
    }

    @Override
    public void apply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        doApply(identity);
    }

    private void doApply(SecurityIdentity identity) {
        if (allowedRoles == null) {
            synchronized (this) {
                if (allowedRoles == null) {
                    allowedRoles = allowedRolesSupplier.get();
                }
            }
        }
        RolesAllowedCheck.doApply(identity, allowedRoles);
    }
}
