package io.quarkus.hibernate.orm.runtime.tenant;

import java.util.Locale;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * Maps from the Quarkus {@link TenantResolver} to the Hibernate {@link CurrentTenantIdentifierResolver} model.
 *
 * @author Michael Schnell
 *
 */
public final class HibernateCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private static final Logger LOG = Logger.getLogger(HibernateCurrentTenantIdentifierResolver.class);

    private final String persistenceUnitName;

    public HibernateCurrentTenantIdentifierResolver(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {

        // Make sure that we're in a request
        if (!Arc.container().requestContext().isActive()) {
            return null;
        }

        TenantResolver resolver = tenantResolver(persistenceUnitName);
        String tenantId = resolver.resolveTenantId();
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

    private static TenantResolver tenantResolver(String persistenceUnitName) {
        InjectableInstance<TenantResolver> instance = PersistenceUnitUtil.legacySingleExtensionInstanceForPersistenceUnit(
                TenantResolver.class, persistenceUnitName);
        if (instance.isUnsatisfied()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "No instance of %1$s was found for persistence unit %2$s. "
                            + "You need to create an implementation for this interface to allow resolving the current tenant identifier.",
                    TenantResolver.class.getSimpleName(), persistenceUnitName));
        }
        return instance.get();
    }

}
