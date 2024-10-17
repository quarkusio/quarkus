package io.quarkus.flyway.multitenant.test;

import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.multitenant.runtime.FlywayPersistenceUnit;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.test.QuarkusUnitTest;

public class NamedPersistenceUnitTest {

    @Inject
    @FlywayPersistenceUnit("named")
    FlywayContainer container;

    @Inject
    @FlywayPersistenceUnit(value = "named", tenantId = "tenant2")
    Flyway tenant2;

    @Inject
    @FlywayPersistenceUnit("named")
    Flyway flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Fruit.class, TestTenantResolver.class)
                    .addAsResource("named-peristence-unit.properties", "application.properties"));

    @Test
    public void testContainer() {
        Assertions.assertTrue(container.isMultiTenancyEnabled());
    }

    @Test
    public void testGettingTenantScopedFlyway() {
        Flyway tenant1 = container.getFlyway("tenant1");
        Assertions.assertNotNull(tenant1);
        Assertions.assertEquals("tenant1", tenant1.getConfiguration().getDefaultSchema());
    }

    @Test
    public void testInjectingTenantScopedFlyway() {
        Assertions.assertNotNull(tenant2);
        Assertions.assertEquals("tenant2", tenant2.getConfiguration().getDefaultSchema());
    }

    @Test
    public void testGettingGlobalFlyway() {
        Flyway flyway = container.getFlyway();
        Assertions.assertNotNull(flyway);
    }

    @Test
    public void testInjectingGlobalFlyway() {
        Assertions.assertNotNull(flyway);
        Assertions.assertNull(flyway.getConfiguration().getDefaultSchema());
    }

    @Entity
    public static class Fruit {
        @Id
        @GeneratedValue
        private Integer id;

        @Column(length = 40, unique = true)
        private String name;

        public Fruit() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @PersistenceUnitExtension("named")
    public static class TestTenantResolver implements TenantResolver {

        @Override
        public String getDefaultTenantId() {
            return "defaultTenantId";
        }

        @Override
        public String resolveTenantId() {
            return "theTenantId";
        }
    }
}
