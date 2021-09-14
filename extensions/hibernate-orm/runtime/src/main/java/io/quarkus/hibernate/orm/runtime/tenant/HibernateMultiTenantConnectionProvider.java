package io.quarkus.hibernate.orm.runtime.tenant;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.hibernate.orm.PersistenceUnit.PersistenceUnitLiteral;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * Maps from the Quarkus {@link TenantConnectionResolver} to the {@link HibernateMultiTenantConnectionProvider} model.
 *
 * @author Michael Schnell
 */
public final class HibernateMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider {

    private static final Logger LOG = Logger.getLogger(HibernateMultiTenantConnectionProvider.class);

    private final String persistenceUnitName;
    private final Map<String, ConnectionProvider> providerMap = new ConcurrentHashMap<>();

    public HibernateMultiTenantConnectionProvider(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        InstanceHandle<TenantResolver> tenantResolver = tenantResolver(persistenceUnitName);
        String tenantId;
        // Activate RequestScope if the TenantResolver is @RequestScoped or @SessionScoped
        ManagedContext requestContext = Arc.container().requestContext();
        Class<? extends Annotation> tenantScope = tenantResolver.getBean().getScope();
        boolean requiresRequestScope = (tenantScope == RequestScoped.class || tenantScope == SessionScoped.class);
        boolean forceRequestActivation = (!requestContext.isActive() && requiresRequestScope);
        try {
            if (forceRequestActivation) {
                requestContext.activate();
            }
            tenantId = tenantResolver.get().getDefaultTenantId();
        } finally {
            if (forceRequestActivation) {
                requestContext.deactivate();
            }
        }
        if (tenantId == null) {
            throw new IllegalStateException("Method 'TenantResolver.getDefaultTenantId()' returned a null value. "
                    + "This violates the contract of the interface!");
        }
        return selectConnectionProvider(tenantId);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(final String tenantIdentifier) {
        LOG.debugv("selectConnectionProvider(persistenceUnitName={0}, tenantIdentifier={1})", persistenceUnitName,
                tenantIdentifier);

        ConnectionProvider provider = providerMap.get(tenantIdentifier);
        if (provider == null) {
            final ConnectionProvider connectionProvider = resolveConnectionProvider(persistenceUnitName, tenantIdentifier);
            providerMap.put(tenantIdentifier, connectionProvider);
            return connectionProvider;
        }
        return provider;

    }

    private static ConnectionProvider resolveConnectionProvider(String persistenceUnitName, String tenantIdentifier) {
        LOG.debugv("resolveConnectionProvider(persistenceUnitName={0}, tenantIdentifier={1})", persistenceUnitName,
                tenantIdentifier);
        // TODO when we switch to the non-legacy method, don't forget to update the definition of the default bean
        //   of type DataSourceTenantConnectionResolver (add the @PersistenceUnitExtension qualifier to that bean)
        InjectableInstance<TenantConnectionResolver> instance = PersistenceUnitUtil
                .legacySingleExtensionInstanceForPersistenceUnit(
                        TenantConnectionResolver.class, persistenceUnitName);
        if (instance.isUnsatisfied()) {
            throw new IllegalStateException(
                    String.format(
                            Locale.ROOT, "No instance of %1$s was found for persistence unit %2$s. "
                                    + "You need to create an implementation for this interface to allow resolving the current tenant connection.",
                            TenantConnectionResolver.class.getSimpleName(), persistenceUnitName));
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
    private static InstanceHandle<TenantResolver> tenantResolver(String persistenceUnitName) {
        InstanceHandle<TenantResolver> resolverInstance;
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            resolverInstance = Arc.container().instance(TenantResolver.class, Default.Literal.INSTANCE);
        } else {
            resolverInstance = Arc.container().instance(TenantResolver.class,
                    new PersistenceUnitLiteral(persistenceUnitName));
        }
        if (!resolverInstance.isAvailable()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "No instance of %1$s was found for persistence unit %2$s. "
                            + "You need to create an implementation for this interface to allow resolving the current tenant identifier.",
                    TenantResolver.class.getSimpleName(), persistenceUnitName));
        }
        return resolverInstance;
    }

}
