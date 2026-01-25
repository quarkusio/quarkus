package io.quarkus.hibernate.reactive.compatibility;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class ORMReactiveCompatbilityDefaultOnlyReactiveDisabledBlockingSessionUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .withConfiguration("""
                    quarkus.hibernate-orm.schema-management.strategy=%s
                    quarkus.hibernate-orm.enabled=false
                    quarkus.datasource.jdbc=false
                    quarkus.datasource.reactive=true
                    quarkus.datasource.db-kind=%s
                    quarkus.datasource.username=%s
                    quarkus.datasource.password=%s
                    """.formatted(SCHEMA_MANAGEMENT_STRATEGY, POSTGRES_KIND, USERNAME_PWD));

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        testReactiveWorks(asserter);
    }

    @Test
    public void testBlockingDisabled() {
        testBlockingDisabled();
    }
}
