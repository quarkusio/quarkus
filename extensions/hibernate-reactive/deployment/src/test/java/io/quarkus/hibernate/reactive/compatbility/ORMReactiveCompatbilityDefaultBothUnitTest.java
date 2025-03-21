package io.quarkus.hibernate.reactive.compatbility;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class ORMReactiveCompatbilityDefaultBothUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ))
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", DATABASE_GENERATION)
            .overrideConfigKey("quarkus.datasource.reactive", "true")
            .overrideConfigKey("quarkus.datasource.reactive.url", POSTGRES_REACTIVE_URL)
            .overrideConfigKey("quarkus.datasource.jdbc.url", POSTGRES_BLOCKING_URL)
            .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        testReactiveWorks(asserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
