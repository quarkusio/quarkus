package io.quarkus.hibernate.orm.multitenancy.database;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies the {@code database} multitenancy strategy with a custom
 * {@link io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver} using a non-{@link String} ({@link Long})
 * tenant identifier.
 * <p>
 * This exercises a path not covered elsewhere: a user-provided <em>generic</em> connection resolver being discovered
 * (by its concrete class) and the {@code Long} tenant identifier flowing from the {@code TenantResolver} through to
 * {@code TenantConnectionResolver.resolve(Long)}. The existing tenancy integration tests are all {@code String}-based,
 * and the discriminator test has no connection resolver at all.
 */
public class DatabaseMultitenancyCustomLongConnectionResolverTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Company.class, DatabaseTenantResolver.class,
                    CustomLongConnectionResolver.class, TenantAccessLog.class, CompanyService.class))
            // The persistence unit gets its connections from the custom resolver, so there is no default datasource
            // and the dialect must be set explicitly (it cannot be inferred from a static datasource).
            .overrideConfigKey("quarkus.datasource.active", "false")
            .overrideConfigKey("quarkus.datasource.tenant.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.tenant.jdbc.url", "jdbc:h2:mem:custom-long-conn")
            .overrideConfigKey("quarkus.hibernate-orm.multitenant", "database")
            .overrideConfigKey("quarkus.hibernate-orm.dialect", "org.hibernate.dialect.H2Dialect")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

    @Inject
    CompanyService companyService;

    @Inject
    TenantAccessLog accessLog;

    @Test
    public void customConnectionResolverIsDiscoveredWithLongTenant() {
        // A successful round-trip proves the custom (generic) TenantConnectionResolver was discovered and used:
        // the default DataSourceTenantConnectionResolver is vetoed once a user bean exists, and would fail anyway.
        companyService.create("Acme");
        assertThat(companyService.listNames()).containsExactly("Acme");

        // And it was invoked with the Long tenant identifier produced by the TenantResolver.
        assertThat(accessLog.resolvedTenants()).contains(DatabaseTenantResolver.TENANT_ID);
    }
}
