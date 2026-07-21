package io.quarkus.hibernate.reactive.compatibility;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class ORMReactiveCompatibilityNamedPersistenceUnitReactiveDisabledUnitTest extends CompatibilityUnitTestBase {

    // The named datasource is available for both jdbc and reactive, so a reactive persistence unit
    // would normally be bootstrapped for the "named-pu" persistence unit too. We explicitly disable it for
    // that PU only, using quarkus.hibernate-orm."named-pu".reactive.enabled=false, while leaving the blocking
    // persistence unit for that same datasource unaffected.
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".datasource", "named-datasource")
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".packages", "io.quarkus.hibernate.reactive.entities")
            .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".reactive.enabled", "false")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD);

    @Inject
    @PersistenceUnit("named-pu")
    SessionFactory namedPersistenceUnitSessionFactory;

    @Test
    public void testBlocking() {
        testBlockingWorks(namedPersistenceUnitSessionFactory);
    }

    @Test
    public void testReactive() {
        testReactiveDisabled("named-pu");
    }
}
