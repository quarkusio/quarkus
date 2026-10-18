package io.quarkus.it.hibernate.search.orm.elasticsearch.multitenancy.book;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

@PersistenceUnitExtension("books")
@RequestScoped
public class BookTenantResolver implements TenantResolver {

    public static final AtomicReference<String> TENANT_ID = new AtomicReference<>("company3");

    @Override
    public String getDefaultTenantId() {
        return "base";
    }

    @Override
    public String resolveTenantId() {
        return TENANT_ID.get();
    }

}
