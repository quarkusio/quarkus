package io.quarkus.hibernate.orm.runtime.tenant;

/**
 * Resolves tenant identifier dynamically so that the proper configuration can
 * be used.
 * <p>
 * This interface is generic and allows using any type as the tenant identifier,
 * for example an {@code enum}, a {@code Long}, or a custom class.
 *
 * @param <T> the type of the tenant identifier
 *
 * @see TenantResolver
 */
public interface TypedTenantResolver<T> extends TenantResolverMarker {

    /**
     * Returns the identifier of the default tenant.
     *
     * @return Default tenant. A non-{@literal null} value is required.
     */
    T getDefaultTenantId();

    /**
     * Returns the current tenant identifier.
     *
     * @return the tenant identifier. This value will be used to select the proper
     *         configuration at runtime. A
     *         non-{@literal null} value is required.
     */
    T resolveTenantId();

    /**
     * Does the given tenant id represent a "root" tenant with access to all
     * partitions?
     *
     * @param tenantId a tenant id produced by {@link #resolveTenantId()}
     *
     * @return true if this is a root tenant
     */
    default boolean isRoot(T tenantId) {
        return false;
    }

}
