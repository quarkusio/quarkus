package io.quarkus.hibernate.orm.runtime.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * Maps from the Quarkus {@link TenantResolver} to the Hibernate {@link CurrentTenantIdentifierResolver} model.
 * 
 * @author Michael Schnell
 *
 */
public class HibernateCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private static final Logger LOG = Logger.getLogger(HibernateCurrentTenantIdentifierResolver.class);

    @Override
    public String resolveCurrentTenantIdentifier() {

        // Make sure that we're in a request
        if (!Arc.container().requestContext().isActive()) {
            return null;
        }

        TenantResolver resolver = tenantResolver();
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
