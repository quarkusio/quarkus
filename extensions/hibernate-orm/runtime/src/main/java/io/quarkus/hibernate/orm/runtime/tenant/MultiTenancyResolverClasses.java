package io.quarkus.hibernate.orm.runtime.tenant;

import java.util.Set;

/**
 * Holds the classes implementing {@link TenantResolver} / {@link TenantConnectionResolver}, collected at build time.
 * <p>
 * These generic interfaces cannot be looked up reliably through Arc typesafe resolution (a raw {@code TenantResolver}
 * required type does not match a bean implementing {@code TenantResolver<NotAWildcard>}), so the beans are selected by
 * their concrete implementation class instead. See
 * {@code io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor#collectTenantResolverClasses} and
 * <a href="https://github.com/quarkusio/quarkus/pull/30447">quarkus#30447</a> for the analogous ValueExtractor
 * workaround.
 * <p>
 * This is an immutable value carried by a synthetic bean, so no mutable static state is needed.
 */
public final class MultiTenancyResolverClasses {

    private final Set<Class<?>> tenantResolverClasses;
    private final Set<Class<?>> tenantConnectionResolverClasses;

    public MultiTenancyResolverClasses(Set<Class<?>> tenantResolverClasses,
            Set<Class<?>> tenantConnectionResolverClasses) {
        this.tenantResolverClasses = tenantResolverClasses;
        this.tenantConnectionResolverClasses = tenantConnectionResolverClasses;
    }

    public Set<Class<?>> tenantResolverClasses() {
        return tenantResolverClasses;
    }

    public Set<Class<?>> tenantConnectionResolverClasses() {
        return tenantConnectionResolverClasses;
    }
}
