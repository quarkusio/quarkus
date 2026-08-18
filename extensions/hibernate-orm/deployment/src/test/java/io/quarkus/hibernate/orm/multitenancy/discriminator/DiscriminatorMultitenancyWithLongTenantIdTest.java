package io.quarkus.hibernate.orm.multitenancy.discriminator;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that a {@link io.quarkus.hibernate.orm.runtime.tenant.TenantResolver} using a non-{@link String} tenant
 * identifier ({@link Long}) is discovered and drives discriminator-based multitenancy end to end.
 * <p>
 * Since implementations of the generic {@code TenantResolver<T>} interface cannot be resolved through a raw
 * {@code TenantResolver.class} Arc lookup, this exercises the build-time collection of implementation classes.
 */
public class DiscriminatorMultitenancyWithLongTenantIdTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Product.class, CurrentTenant.class, LongTenantResolver.class,
                    ProductService.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:tenant-long")
            .overrideConfigKey("quarkus.hibernate-orm.multitenant", "discriminator")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

    @Inject
    ProductService productService;

    @Inject
    CurrentTenant currentTenant;

    @Test
    public void nonStringTenantIdentifierIsResolvedAndIsolatesData() {
        currentTenant.set(1L);
        productService.create("apple");
        productService.create("banana");

        currentTenant.set(2L);
        productService.create("carrot");

        // The Long tenant identifier must be persisted into the discriminator column as-is.
        assertThat(productService.tenantIdOfSingleProduct()).isEqualTo(2L);

        // Data must be isolated per (Long) tenant.
        currentTenant.set(1L);
        assertThat(productService.listNames()).containsExactly("apple", "banana");

        currentTenant.set(2L);
        assertThat(productService.listNames()).containsExactly("carrot");
    }
}
