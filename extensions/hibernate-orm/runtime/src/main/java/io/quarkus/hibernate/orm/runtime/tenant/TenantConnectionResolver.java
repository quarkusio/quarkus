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
     * @param tenantId the tenant identifier. If {@code null}, indicates that the default configuration/tenant should be chosen.
     * @return Hibernate connection provider for the current provider. Never {@literal null}.
     */
    ConnectionProvider resolve(String tenantId);

}
