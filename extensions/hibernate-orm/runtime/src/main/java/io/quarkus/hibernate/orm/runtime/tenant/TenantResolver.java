package io.quarkus.hibernate.orm.runtime.tenant;

import io.vertx.ext.web.RoutingContext;

/**
 * A tenant resolver is responsible for resolving tenants dynamically so that the proper configuration can be used accordingly.
 * 
 * @author Michael Schnell
 *
 */
public interface TenantResolver {

    /**
     * Returns the identifier of the default tenant.
     * 
     * @return Default tenant.
     */
    String getDefaultHibernateOrmTenantId();

    /**
     * Returns a tenant identifier given a {@code RoutingContext}, where the identifier will be used to choose the proper
     * configuration during runtime.
     * 
     * @param context the routing context. Required value that cannot be {@literal null},
     * @return the tenant identifier. A non-{@literal null} value is required.
     */
    String resolveHibernateOrmTenantId(RoutingContext context);

}
