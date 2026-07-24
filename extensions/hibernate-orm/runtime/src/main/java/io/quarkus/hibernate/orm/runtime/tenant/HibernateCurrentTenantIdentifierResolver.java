package io.quarkus.hibernate.orm.runtime.tenant;

import java.util.Locale;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * Maps from the Quarkus {@link TenantResolver} to the Hibernate {@link CurrentTenantIdentifierResolver} model.
 * <p>
 * The tenant identifier type is intentionally erased to {@link Object} here: the actual type is defined by the
 * user-provided {@link TenantResolver} implementation and Hibernate ORM treats the identifier as an opaque value.
 *
 * @author Michael Schnell
 *
 */
public final class HibernateCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<Object> {

    private static final Logger LOG = Logger.getLogger(HibernateCurrentTenantIdentifierResolver.class);

    private final String persistenceUnitName;

    // Cached once (lazily, at first resolution) to avoid repeating the container lookup on every tenant resolution.
    private volatile MultiTenancyResolverClasses resolverClasses;

    public HibernateCurrentTenantIdentifierResolver(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public Object resolveCurrentTenantIdentifier() {

        // Make sure that we're in a request
        if (!Arc.container().requestContext().isActive()) {
            return null;
        }

        TenantResolver<Object> resolver = tenantResolver(persistenceUnitName);
        Object tenantId = resolver.resolveTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Method 'TenantResolver.resolveTenantId()' returned a null value. "
                    + "Unfortunately Hibernate ORM does not allow null for tenant identifiers. "
                    + "Please use a non-null value!");
        }
        LOG.debugv("resolveCurrentTenantIdentifier(): {0}", tenantId);
        return tenantId;

    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(Object tenantId) {
        // Make sure that we're in a request
        if (!Arc.container().requestContext().isActive()) {
            return false;
        }
        TenantResolver<Object> resolver = tenantResolver(persistenceUnitName);
        if (resolver == null) {
            return false;
        }
        return resolver.isRoot(tenantId);
    }

    private TenantResolver<Object> tenantResolver(String persistenceUnitName) {
        return PersistenceUnitUtil.singleTenantResolver(resolverClasses().tenantResolverClasses(), persistenceUnitName)
                .map(InstanceHandle::get)
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
