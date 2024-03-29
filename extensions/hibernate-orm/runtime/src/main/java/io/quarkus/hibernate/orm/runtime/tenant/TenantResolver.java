package io.quarkus.hibernate.orm.runtime.tenant;

/**
 * Resolves tenant identifier dynamically so that the proper configuration can be used.
 *
 * @author Michael Schnell
 *
 */
public interface TenantResolver {

    /**
     * Returns the identifier of the default tenant.
     *
     * @return Default tenant.A non-{@literal null} value is required.
     */
    String getDefaultTenantId();

    /**
     * Returns the current tenant identifier.
     *
     * @return the tenant identifier. This value will be used to select the proper configuration at runtime. A
     *         non-{@literal null} value is required.
     */
    String resolveTenantId();

    /**
     * Does the given tenant id represent a "root" tenant with access to all partitions?
     *
     * @param tenantId a tenant id produced by {@link #resolveTenantId()}
     *
     * @return true is this is root tenant
     */
    default boolean isRoot(String tenantId) {
        return false;
    }

}
