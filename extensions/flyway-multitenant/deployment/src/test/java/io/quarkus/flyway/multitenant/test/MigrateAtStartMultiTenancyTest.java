package io.quarkus.flyway.multitenant.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayTenantSupport;
import io.quarkus.flyway.multitenant.runtime.FlywayPersistenceUnit;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.test.QuarkusUnitTest;

public class MigrateAtStartMultiTenancyTest {

    @Inject
    @FlywayPersistenceUnit(tenantId = "tenant1")
    Flyway tenant1;

    @Inject
    @FlywayPersistenceUnit(tenantId = "tenant2")
    Flyway tenant2;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Fruit.class, TestTenantResolver.class, TenantToInitialize.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("migrate-at-start-multitenancy.properties", "application.properties"));

    @Test
    public void testMigrateAtStart() {
        MigrationInfo migrationInfo = tenant1.info().current();
        assertNotNull(migrationInfo, "No Flyway migration was executed on tenant 1");

        String currentVersion = migrationInfo
                .getVersion()
                .toString();

        assertEquals("1.0.0", currentVersion);

        migrationInfo = tenant2.info().current();
        assertNotNull(migrationInfo, "No Flyway migration was executed on tenant 2");

        currentVersion = migrationInfo
                .getVersion()
                .toString();

        assertEquals("1.0.0", currentVersion);
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

    @PersistenceUnitExtension()
    public static class TestTenantResolver implements TenantResolver {

        @Override
        public String getDefaultTenantId() {
            return "tenant1";
        }

        @Override
        public String resolveTenantId() {
            return "tenant2";
        }
    }

    @FlywayPersistenceUnit
    public static class TenantToInitialize implements FlywayTenantSupport {

        @Override
        public List<String> getTenantsToInitialize() {
            return List.of("tenant1", "tenant2");
        }
    }
}
