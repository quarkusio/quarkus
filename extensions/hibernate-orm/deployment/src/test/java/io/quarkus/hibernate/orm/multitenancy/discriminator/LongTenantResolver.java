package io.quarkus.hibernate.orm.multitenancy.discriminator;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

/**
 * A {@link TenantResolver} using a non-{@link String} tenant identifier ({@link Long}).
 * <p>
 * This exercises the generic bean discovery: the bean's only {@code TenantResolver} bean type is the parameterized
 * {@code TenantResolver<Long>}, so it can only be found through the build-time-collected implementation classes.
 */
@PersistenceUnitExtension
@RequestScoped
public class LongTenantResolver implements TenantResolver<Long> {

    @Inject
    CurrentTenant currentTenant;

    @Override
    public Long getDefaultTenantId() {
        return 0L;
    }

    @Override
    public Long resolveTenantId() {
        return currentTenant.get();
    }

    @Override
    public boolean isRoot(Long tenantId) {
        // No tenant used in this test is negative, so this is always false;
        // it exists only to prove that isRoot(T) with a non-String type compiles and is invoked.
        return tenantId != null && tenantId < 0;
    }
}
