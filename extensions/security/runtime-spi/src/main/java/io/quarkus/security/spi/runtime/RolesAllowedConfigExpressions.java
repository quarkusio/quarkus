package io.quarkus.security.spi.runtime;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Suppliers of the roles resolved from the configuration expressions that extensions accept in place of role names,
 * for example {@code @SecureField(rolesAllowed = "${admin-role}")}.
 * <p>
 * The Quarkus Security extension registers this type as a static-init service when at least one
 * {@code io.quarkus.security.spi.RolesAllowedConfigExpResolverBuildItem} was produced. The suppliers resolve the
 * expressions from the runtime configuration, therefore they must only be invoked when it is available.
 */
public final class RolesAllowedConfigExpressions {

    private final Map<String, Supplier<String[]>> rolesSuppliers;

    public RolesAllowedConfigExpressions(Map<String, Supplier<String[]>> rolesSuppliers) {
        this.rolesSuppliers = Map.copyOf(rolesSuppliers);
    }

    /**
     * @return suppliers of the allowed roles keyed by the configuration expression they resolve
     */
    public Map<String, Supplier<String[]>> rolesSuppliers() {
        return rolesSuppliers;
    }
}
