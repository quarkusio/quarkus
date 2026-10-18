package io.quarkus.it.jpa.generatedvalue;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

@PersistenceUnitExtension
@RequestScoped
public class CustomTenantResolver implements TenantResolver {

    private static final String DEFAULT_TENANT = "default";

    @Override
    public String getDefaultTenantId() {
        return DEFAULT_TENANT;
    }

    @Override
    public String resolveTenantId() {
        return DEFAULT_TENANT;
    }

}
