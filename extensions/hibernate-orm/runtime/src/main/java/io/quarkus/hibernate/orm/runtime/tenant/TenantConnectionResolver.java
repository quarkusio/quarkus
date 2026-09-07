package io.quarkus.hibernate.orm.runtime.tenant;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Resolves the {@link ConnectionProvider} for tenants dynamically.
 *
 * @param <T> the type of the tenant identifier. Use {@link String} unless you rely on a custom tenant identifier type
 *        supported by Hibernate ORM.
 *
 * @author Michael Schnell
 *
 */
public interface TenantConnectionResolver<T> {

    /**
     * Returns a connection provider for the current tenant based on the context.
     *
     * @param tenantId the tenant identifier. Required value that cannot be {@literal null}.
     * @return Hibernate connection provider for the current provider. A non-{@literal null} value is required.
     */
    ConnectionProvider resolve(T tenantId);

}
