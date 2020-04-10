package io.quarkus.hibernate.orm.runtime.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.vertx.ext.web.RoutingContext;

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

        TenantResolver resolver = tenantResolver();

        // Make sure that we're in a request or return default tenant
        if (!Arc.container().requestContext().isActive()) {
            return resolver.getDefaultHibernateOrmTenantId();
        }

        RoutingContext routingContext = routingContext();
        String tenantId = resolver.resolveHibernateOrmTenantId(routingContext);
        if (tenantId == null) {
            throw new IllegalStateException("Method 'CurrentTenantIdentifierResolver."
                    + "resolveHibernateOrmTenantId(RoutingContext)' returned a null value. "
                    + "Unfortunately Hibernate ORM does not allow null for tenant identifiers. "
                    + "Please use a non-null value!");
        }
        LOG.debugv("resolveCurrentTenantIdentifier(): {0}", tenantId);
        return tenantId;

    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return jpaConfig().isValidateTenantInCurrentSessions();
    }

    /**
     * Returns the singleton JPA configuration instance.
     * 
     * @return JPA configuration.
     */
    private static JPAConfig jpaConfig() {
        InstanceHandle<JPAConfig> jpaConfigInstance = Arc.container().instance(JPAConfig.class);
        if (!jpaConfigInstance.isAvailable()) {
            throw new IllegalStateException("No instance of JPAConfig found");
        }
        return jpaConfigInstance.get();
    }

    /**
     * Retrieves the routing context or fails if it is not available.
     * 
     * @return Current routing context.
     */
    private static RoutingContext routingContext() {
        InstanceHandle<RoutingContext> routingContextInstance = Arc.container().instance(RoutingContext.class);
        if (!routingContextInstance.isAvailable()) {
            throw new IllegalStateException("No instance of " + RoutingContext.class.getName() + " was found.");
        }
        return routingContextInstance.get();
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
