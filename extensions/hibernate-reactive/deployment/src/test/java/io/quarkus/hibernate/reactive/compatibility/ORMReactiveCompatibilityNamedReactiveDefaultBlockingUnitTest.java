package io.quarkus.hibernate.reactive.compatibility;

import java.util.List;

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

public class ORMReactiveCompatibilityNamedReactiveDefaultBlockingUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ))
            .withConfigurationResource("application-unittest-both-named-reactive-and-default-blocking.properties")
            // To have the named PU is reactive we need to explicitly disable the blocking the JDBC data source
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".jdbc", "false")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".datasource", "named-datasource")
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".packages", "io.quarkus.hibernate.reactive.entities")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD)
            // Default blocking will need to disable the reactive one, JDBC is enabled by default.
            .overrideConfigKey("quarkus.datasource.reactive", "false")
            // In such case packages for the default PU should be explicit as well
            .overrideConfigKey("quarkus.hibernate-orm.packages", "io.quarkus.hibernate.reactive.entities")
            .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.hibernate\".level", "DEBUG");

    @PersistenceUnit("named-pu")
    Mutiny.SessionFactory namedMutinySessionFactory;

    @Test
    @RunOnVertxContext
    public void test(UniAsserter uniAsserter) {
        testReactiveWorks(namedMutinySessionFactory, uniAsserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
