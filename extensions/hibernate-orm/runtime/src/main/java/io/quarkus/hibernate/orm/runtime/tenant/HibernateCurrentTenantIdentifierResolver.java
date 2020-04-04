package io.quarkus.hibernate.orm.runtime.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.vertx.ext.web.RoutingContext;

/**
 * Maps from the Quarkus {@link TenantResolver} to the Hibernate {@link CurrentTenantIdentifierResolver} model.
 * 
 * @author Michael Schnell
 *
 */
public class HibernateCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        InstanceHandle<TenantResolver> resolverInstance = Arc.container().instance(TenantResolver.class);
        if (!resolverInstance.isAvailable()) {
            throw new IllegalStateException("No instance of " + TenantResolver.class.getName() + " was found. "
                    + "You need to create an implementation for this interface to allow resolving the current tenant identifier.");
        }
        TenantResolver resolver = resolverInstance.get();
        InstanceHandle<RoutingContext> routingContextInstance = Arc.container().instance(RoutingContext.class);
        if (!routingContextInstance.isAvailable()) {
            throw new IllegalStateException("No instance of " + RoutingContext.class.getName() + " was found.");
        }
        RoutingContext routingContext = routingContextInstance.get();
        return resolver.resolve(routingContext);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // TODO Get from config!
        return false;
    }

}
