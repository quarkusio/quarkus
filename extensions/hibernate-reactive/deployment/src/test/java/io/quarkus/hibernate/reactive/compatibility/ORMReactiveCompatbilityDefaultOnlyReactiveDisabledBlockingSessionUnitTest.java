package io.quarkus.hibernate.reactive.compatibility;

import java.io.IOException;
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

    // We disable the blocking data source but keep the persistence unit by using the quarkus.hibernate-orm.blocking property
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .withConfigurationResource("application-unittest-onlyreactive.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.blocking", "false")
            .overrideConfigKey("quarkus.datasource.reactive", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        testReactiveWorks(asserter);
    }

    @Test
    public void testBlocking() throws IOException {
        testBlockingDisabled();
    }
}
