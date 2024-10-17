package io.quarkus.flyway.multitenant.test;

import jakarta.enterprise.inject.spi.CDI;
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
import io.quarkus.flyway.multitenant.runtime.FlywayPersistenceUnit.FlywayPersistenceUnitLiteral;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.test.QuarkusUnitTest;

public class NoMultiTenancyTest {

    @Inject
    @FlywayPersistenceUnit()
    FlywayContainer container;

    @Inject
    @FlywayPersistenceUnit()
    Flyway flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Fruit.class)
                    .addAsResource("no-multitenancy.properties", "application.properties"));

    @Test
    public void testContainer() {
        Assertions.assertFalse(container.isMultiTenancyEnabled());
    }

    @Test
    public void testInjectingGlobalFlyway() {
        Assertions.assertNotNull(flyway);
        Assertions.assertNull(flyway.getConfiguration().getDefaultSchema());
    }

    @Test
    public void testInjectingTenantScopedFlywayIsFailing() {
        Assertions.assertThrows(RuntimeException.class,
                () -> CDI.current().select(Flyway.class, FlywayPersistenceUnitLiteral.of("tenant2")).get());

    }

    @Test
    public void testGettingTenantScopedFlywayIsFailing() {
        Assertions.assertThrows(RuntimeException.class, () -> container.getFlyway("tenant1"));
    }

    @Test
    public void testGettingGlobalFlyway() {
        Flyway flyway = container.getFlyway();
        Assertions.assertNotNull(flyway);
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
}
