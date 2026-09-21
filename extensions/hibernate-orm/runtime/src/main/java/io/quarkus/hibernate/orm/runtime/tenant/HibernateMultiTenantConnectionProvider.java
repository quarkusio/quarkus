package io.quarkus.hibernate.orm.runtime.tenant;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * Maps from the Quarkus {@link TenantConnectionResolver} to the {@link HibernateMultiTenantConnectionProvider} model.
 * <p>
 * The tenant identifier type is intentionally erased to {@link Object} here: the actual type is defined by the
 * user-provided {@link TenantResolver} / {@link TenantConnectionResolver} implementations and Hibernate ORM treats the
 * identifier as an opaque value.
 *
 * @author Michael Schnell
 */
public final class HibernateMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider<Object> {

    private static final Logger LOG = Logger.getLogger(HibernateMultiTenantConnectionProvider.class);

    private final String persistenceUnitName;
    private final Map<Object, ConnectionProvider> providerMap = new ConcurrentHashMap<>();

    // Cached once (lazily, at first resolution) to avoid repeating the container lookup on every tenant resolution.
    private volatile MultiTenancyResolverClasses resolverClasses;

    public HibernateMultiTenantConnectionProvider(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        InstanceHandle<TenantResolver<Object>> tenantResolver = tenantResolver(persistenceUnitName);
        Object tenantId;
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
    protected ConnectionProvider selectConnectionProvider(final Object tenantIdentifier) {
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

    private ConnectionProvider resolveConnectionProvider(String persistenceUnitName, Object tenantIdentifier) {
        LOG.debugv("resolveConnectionProvider(persistenceUnitName={0}, tenantIdentifier={1})", persistenceUnitName,
                tenantIdentifier);
        InstanceHandle<TenantConnectionResolver<Object>> instance = PersistenceUnitUtil
                .singleTenantConnectionResolver(resolverClasses().tenantConnectionResolverClasses(), persistenceUnitName)
                .orElseThrow(() -> new IllegalStateException(String.format(Locale.ROOT,
                        "No instance of %1$s was found for persistence unit %2$s. "
                                + "You need to create an implementation for this interface to allow resolving the current tenant connection.",
                        TenantConnectionResolver.class.getSimpleName(), persistenceUnitName)));
        ConnectionProvider cp = instance.get().resolve(tenantIdentifier);
        if (cp == null) {
            throw new IllegalStateException("Method 'TenantConnectionResolver."
                    + "resolve(Object)' returned a null value. This violates the contract of the interface!");
        }
        return cp;
    }

    /**
     * Retrieves the tenant resolver or fails if it is not available.
     *
     * @return Current tenant resolver.
     */
    private InstanceHandle<TenantResolver<Object>> tenantResolver(String persistenceUnitName) {
        return PersistenceUnitUtil.singleTenantResolver(resolverClasses().tenantResolverClasses(), persistenceUnitName)
                .orElseThrow(() -> new IllegalStateException(String.format(Locale.ROOT,
                        "No instance of %1$s was found for persistence unit %2$s. "
                                + "You need to create an implementation for this interface to allow resolving the current tenant identifier.",
                        TenantResolver.class.getSimpleName(), persistenceUnitName)));
    }

    private MultiTenancyResolverClasses resolverClasses() {
        MultiTenancyResolverClasses local = resolverClasses;
        if (local == null) {
            local = Arc.container().select(MultiTenancyResolverClasses.class).get();
            resolverClasses = local;
        }
        return local;
    }

}
