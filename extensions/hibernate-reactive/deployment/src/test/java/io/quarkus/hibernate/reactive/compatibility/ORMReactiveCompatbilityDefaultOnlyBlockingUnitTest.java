package io.quarkus.hibernate.reactive.compatibility;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;

public class ORMReactiveCompatbilityDefaultOnlyBlockingUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ))
            .withConfigurationResource("application-unittest-onlyblocking.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.datasource.reactive", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);

    @Test
    @RunOnVertxContext
    public void testReactive() {
        testReactiveDisabled();
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
