package io.quarkus.hibernate.orm.runtime.tenant;

/**
 * Resolves tenant identifier dynamically so that the proper configuration can be used.
 *
 * @param <T> the type of the tenant identifier. Use {@link String} unless you rely on a custom tenant identifier type
 *        supported by Hibernate ORM.
 *
 * @author Michael Schnell
 *
 */
public interface TenantResolver<T> {

    /**
     * Returns the identifier of the default tenant.
     *
     * @return Default tenant.A non-{@literal null} value is required.
     */
    T getDefaultTenantId();

    /**
     * Returns the current tenant identifier.
     *
     * @return the tenant identifier. This value will be used to select the proper configuration at runtime. A
     *         non-{@literal null} value is required.
     */
    T resolveTenantId();

    /**
     * Does the given tenant id represent a "root" tenant with access to all partitions?
     *
     * @param tenantId a tenant id produced by {@link #resolveTenantId()}
     *
     * @return true is this is root tenant
     */
    default boolean isRoot(T tenantId) {
        return false;
    }

}
