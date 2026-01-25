package io.quarkus.hibernate.reactive.compatibility;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class ORMReactiveCompatbilityDefaultOnlyBlockingUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .withConfiguration("""
                    quarkus.hibernate-orm.schema-management.strategy=%s
                    quarkus.datasource.reactive=false
                    quarkus.datasource.db-kind=%s
                    quarkus.datasource.username=%s
                    quarkus.datasource.password=%s
                    """.formatted(SCHEMA_MANAGEMENT_STRATEGY, POSTGRES_KIND, USERNAME_PWD));

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }

    @Test
    public void testReactiveDisabled() {
        testReactiveDisabled();
    }
}
