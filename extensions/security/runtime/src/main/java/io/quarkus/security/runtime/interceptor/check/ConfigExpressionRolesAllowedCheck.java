package io.quarkus.security.runtime.interceptor.check;

import java.util.function.Supplier;

/**
 * Roles allowed check whose roles contain configuration expressions that are resolved from the runtime configuration.
 */
public final class ConfigExpressionRolesAllowedCheck extends SupplierRolesAllowedCheck {

    public ConfigExpressionRolesAllowedCheck(Supplier<String[]> allowedRolesSupplier) {
        super(allowedRolesSupplier);
    }
}
