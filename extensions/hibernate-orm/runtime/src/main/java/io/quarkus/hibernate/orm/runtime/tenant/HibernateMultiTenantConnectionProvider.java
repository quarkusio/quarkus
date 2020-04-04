package io.quarkus.hibernate.orm.runtime.tenant;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

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

    private static final String DEFAULT = "default";

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return selectConnectionProvider(DEFAULT);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        InstanceHandle<TenantConnectionResolver> instance = Arc.container().instance(TenantConnectionResolver.class);
        if (!instance.isAvailable()) {
            throw new IllegalStateException(
                    "No instance of " + TenantConnectionResolver.class.getSimpleName() + " was found. "
                            + "This shouldn't happen, as the " + DataSourceTenantConnectionResolver.class.getSimpleName()
                            + " is always available");
        }
        TenantConnectionResolver resolver = instance.get();
        return resolver.resolve(tenantIdentifier);
    }

}
