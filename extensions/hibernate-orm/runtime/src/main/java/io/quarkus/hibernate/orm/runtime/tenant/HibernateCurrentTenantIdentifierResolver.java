package io.quarkus.hibernate.orm.runtime.tenant;

import java.util.Locale;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * Maps from the Quarkus {@link TypedTenantResolver}
 * to the Hibernate {@link CurrentTenantIdentifierResolver} model.
 *
 */
public final class HibernateCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<Object> {

    private static final Logger LOG = Logger.getLogger(HibernateCurrentTenantIdentifierResolver.class);

    private final String persistenceUnitName;

    public HibernateCurrentTenantIdentifierResolver(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public Object resolveCurrentTenantIdentifier() {

        // Make sure that we're in a request
        if (!Arc.container().requestContext().isActive()) {
            return null;
        }

        Object tenantId = tenantResolver(persistenceUnitName).resolveTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Method 'resolveTenantId()' returned a null value. "
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
        return tenantResolver(persistenceUnitName).isRoot(tenantId);
    }

    private static TypedTenantResolver tenantResolver(String persistenceUnitName) {
        InjectableInstance<TenantResolverMarker> instance = PersistenceUnitUtil
                .legacySingleExtensionInstanceForPersistenceUnit(
                        TenantResolverMarker.class, persistenceUnitName);
        if (instance.isUnsatisfied()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "No instance of %1$s was found for persistence unit %2$s. "
                            + "You need to create an implementation for this interface to allow resolving the current tenant identifier.",
                    TypedTenantResolver.class.getSimpleName(), persistenceUnitName));
        }
        TenantResolverMarker tenantResolver = instance.get();
        if (tenantResolver instanceof TypedTenantResolver typedTenantResolver) {
            return typedTenantResolver;
        } else {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "An instance of %1$s was found for persistence unit %2$s "
                            + "which is not a subtype of %3$s. You need to create an implementation for this interface to allow resolving the current tenant identifier.",
                    TenantResolverMarker.class.getSimpleName(), persistenceUnitName,
                    TypedTenantResolver.class.getSimpleName()));
        }
    }

}
