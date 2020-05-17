package io.quarkus.hibernate.orm.runtime.tenant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * Maps from the Quarkus {@link TenantConnectionResolver} to the {@link HibernateMultiTenantConnectionProvider} model.
 * 
 * @author Michael Schnell
 *
 */
public final class HibernateMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider {

    private static final Logger LOG = Logger.getLogger(HibernateMultiTenantConnectionProvider.class);

    private final Map<String, ConnectionProvider> providerMap = new ConcurrentHashMap<>();

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        String tenantId = tenantResolver().getDefaultTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Method 'TenantResolver.getDefaultTenantId()' returned a null value. "
                    + "This violates the contract of the interface!");
        }
        return selectConnectionProvider(tenantId);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(final String tenantIdentifier) {
        LOG.debugv("selectConnectionProvider({0})", tenantIdentifier);

        ConnectionProvider provider = providerMap.get(tenantIdentifier);
        if (provider == null) {
            final ConnectionProvider connectionProvider = resolveConnectionProvider(tenantIdentifier);
            providerMap.put(tenantIdentifier, connectionProvider);
            return connectionProvider;
        }
        return provider;

    }

    private static ConnectionProvider resolveConnectionProvider(String tenantIdentifier) {
        LOG.debugv("resolveConnectionProvider({0})", tenantIdentifier);
        InstanceHandle<TenantConnectionResolver> instance = Arc.container().instance(TenantConnectionResolver.class);
        if (!instance.isAvailable()) {
            throw new IllegalStateException(
                    "No instance of " + TenantConnectionResolver.class.getSimpleName() + " was found. "
                            + "You need to create an implementation for this interface to allow resolving the current tenant connection.");
        }
        TenantConnectionResolver resolver = instance.get();
        ConnectionProvider cp = resolver.resolve(tenantIdentifier);
        if (cp == null) {
            throw new IllegalStateException("Method 'TenantConnectionResolver."
                    + "resolve(String)' returned a null value. This violates the contract of the interface!");
        }
        return cp;
    }

    /**
     * Retrieves the tenant resolver or fails if it is not available.
     * 
     * @return Current tenant resolver.
     */
    private static TenantResolver tenantResolver() {
        InstanceHandle<TenantResolver> resolverInstance = Arc.container().instance(TenantResolver.class);
        if (!resolverInstance.isAvailable()) {
            throw new IllegalStateException("No instance of " + TenantResolver.class.getName() + " was found. "
                    + "You need to create an implementation for this interface to allow resolving the current tenant identifier.");
        }
        return resolverInstance.get();
    }

}
