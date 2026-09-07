package io.quarkus.hibernate.orm.multitenancy.database;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

/**
 * A {@link TenantResolver} using a non-{@link String} tenant identifier ({@link Long}), so the {@code Long} flows into
 * the custom {@link CustomLongConnectionResolver}.
 */
@PersistenceUnitExtension
@ApplicationScoped
public class DatabaseTenantResolver implements TenantResolver<Long> {

    static final Long TENANT_ID = 7L;

    @Override
    public Long getDefaultTenantId() {
        return TENANT_ID;
    }

    @Override
    public Long resolveTenantId() {
        return TENANT_ID;
    }
}
