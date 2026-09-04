package io.quarkus.hibernate.orm.multitenancy.database;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Observable record of the tenant identifiers the custom {@link CustomLongConnectionResolver} was asked to resolve.
 * <p>
 * A separate {@code @Default} bean is used (rather than injecting the resolver directly) because the resolver carries
 * the {@code @PersistenceUnitExtension} qualifier and therefore is not a {@code @Default} bean.
 */
@ApplicationScoped
public class TenantAccessLog {

    private final List<Long> resolvedTenants = new CopyOnWriteArrayList<>();

    public void record(Long tenantId) {
        resolvedTenants.add(tenantId);
    }

    public List<Long> resolvedTenants() {
        return resolvedTenants;
    }
}
