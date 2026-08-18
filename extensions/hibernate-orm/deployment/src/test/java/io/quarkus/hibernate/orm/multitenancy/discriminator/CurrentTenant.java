package io.quarkus.hibernate.orm.multitenancy.discriminator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Holds the tenant identifier selected by the test. Kept application-scoped so the test can switch tenants across
 * request boundaries.
 */
@ApplicationScoped
public class CurrentTenant {

    private volatile Long tenantId = 0L;

    public Long get() {
        return tenantId;
    }

    public void set(Long tenantId) {
        this.tenantId = tenantId;
    }
}
