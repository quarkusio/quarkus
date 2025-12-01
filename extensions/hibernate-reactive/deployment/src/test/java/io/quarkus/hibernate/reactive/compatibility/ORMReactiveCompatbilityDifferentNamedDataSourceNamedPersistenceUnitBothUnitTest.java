package io.quarkus.hibernate.reactive.compatibility;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class ORMReactiveCompatbilityDifferentNamedDataSourceNamedPersistenceUnitBothUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ))
            // Reactive named datasource
            .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".jdbc", "false")
            .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".reactive", "true")
            .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".password", USERNAME_PWD)
            // Reactive named persistence unit
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-reactive\".schema-management.strategy",
                    SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-reactive\".datasource", "named-datasource-reactive")
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-reactive\".packages", "io.quarkus.hibernate.reactive.entities")

            // Blocking named datasource
            .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".jdbc", "true")
            .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".reactive", "false")
            .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".password", USERNAME_PWD)
            // Blocking named persistence unit
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-blocking\".schema-management.strategy",
                    SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-blocking\".datasource", "named-datasource-blocking")
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-blocking\".packages", "io.quarkus.hibernate.reactive.entities")

            .overrideConfigKey("quarkus.log.category.\"io.quarkus.hibernate\".level", "DEBUG");

    @PersistenceUnit("named-pu-reactive")
    Mutiny.SessionFactory namedMutinySessionFactory;

    @Test
    @RunOnVertxContext
    public void test(UniAsserter uniAsserter) {
        testReactiveWorks(namedMutinySessionFactory, uniAsserter);
    }

    @Inject
    @PersistenceUnit("named-pu-blocking")
    SessionFactory namedPersistenceUnitSessionFactory;

    @Test
    public void testBlocking() {
        testBlockingWorks(namedPersistenceUnitSessionFactory);
    }
}