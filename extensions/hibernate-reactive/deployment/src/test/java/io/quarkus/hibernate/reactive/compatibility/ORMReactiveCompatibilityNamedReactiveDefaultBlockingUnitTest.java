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
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .withConfiguration("""
                    quarkus.datasource."named-datasource".jdbc=false
                    quarkus.datasource."named-datasource".reactive=true
                    quarkus.hibernate-orm."named-pu".schema-management.strategy=%s
                    quarkus.hibernate-orm."named-pu".datasource=named-datasource
                    quarkus.hibernate-orm."named-pu".packages=io.quarkus.hibernate.reactive.entities
                    quarkus.datasource."named-datasource".db-kind=%s
                    quarkus.datasource."named-datasource".username=%s
                    quarkus.datasource."named-datasource".password=%s
                    quarkus.datasource.reactive=false
                    quarkus.datasource.db-kind=%s
                    quarkus.datasource.username=%s
                    quarkus.datasource.password=%s
                    quarkus.hibernate-orm.schema-management.strategy=%s
                    quarkus.hibernate-orm.packages=io.quarkus.hibernate.reactive.entities
                    """.formatted(SCHEMA_MANAGEMENT_STRATEGY, POSTGRES_KIND, USERNAME_PWD, USERNAME_PWD,
                    POSTGRES_KIND, USERNAME_PWD, USERNAME_PWD, SCHEMA_MANAGEMENT_STRATEGY));

    @PersistenceUnit("named-pu")
    Mutiny.SessionFactory namedReactiveSessionFactory;

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        testReactiveWorks(namedReactiveSessionFactory, asserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
