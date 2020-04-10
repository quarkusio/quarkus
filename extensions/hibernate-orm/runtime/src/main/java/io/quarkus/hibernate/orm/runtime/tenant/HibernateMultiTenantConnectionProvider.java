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
public class HibernateMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(HibernateMultiTenantConnectionProvider.class);

    private static final Map<String, ConnectionProvider> PROVIDER_MAP = new ConcurrentHashMap<>();

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return selectConnectionProvider(tenantResolver().getDefaultHibernateOrmTenantId());
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        LOG.debugv("selectConnectionProvider({0})", tenantIdentifier);

        return PROVIDER_MAP.computeIfAbsent(tenantIdentifier, tid -> {
            return resolveConnectionProvider(tid);
        });

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
