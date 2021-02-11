package io.quarkus.hibernate.orm.runtime.tenant;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Resolves the {@link ConnectionProvider} for tenants dynamically.
 * 
 * @author Michael Schnell
 *
 */
public interface TenantConnectionResolver {

    /**
     * Returns a connection provider for the current tenant based on the context.
     * 
     * @param tenantId the tenant identifier. Required value that cannot be {@literal null}.
     * @return Hibernate connection provider for the current provider. A non-{@literal null} value is required.
     */
    ConnectionProvider resolve(String tenantId);

}
