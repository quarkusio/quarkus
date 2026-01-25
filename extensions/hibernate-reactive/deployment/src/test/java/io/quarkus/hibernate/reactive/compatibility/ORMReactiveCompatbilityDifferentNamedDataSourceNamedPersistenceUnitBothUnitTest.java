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
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .withConfiguration("""
                    quarkus.datasource."named-datasource-reactive".jdbc=false
                    quarkus.datasource."named-datasource-reactive".reactive=true
                    quarkus.datasource."named-datasource-reactive".db-kind=%s
                    quarkus.datasource."named-datasource-reactive".username=%s
                    quarkus.datasource."named-datasource-reactive".password=%s
                    quarkus.hibernate-orm."named-pu-reactive".schema-management.strategy=%s
                    quarkus.hibernate-orm."named-pu-reactive".datasource=named-datasource-reactive
                    quarkus.hibernate-orm."named-pu-reactive".packages=io.quarkus.hibernate.reactive.entities
                    quarkus.datasource."named-datasource-blocking".jdbc=true
                    quarkus.datasource."named-datasource-blocking".reactive=false
                    quarkus.datasource."named-datasource-blocking".db-kind=%s
                    quarkus.datasource."named-datasource-blocking".username=%s
                    quarkus.datasource."named-datasource-blocking".password=%s
                    quarkus.hibernate-orm."named-pu-blocking".schema-management.strategy=%s
                    quarkus.hibernate-orm."named-pu-blocking".datasource=named-datasource-blocking
                    quarkus.hibernate-orm."named-pu-blocking".packages=io.quarkus.hibernate.reactive.entities
                    quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                    """.formatted(POSTGRES_KIND, USERNAME_PWD, USERNAME_PWD, SCHEMA_MANAGEMENT_STRATEGY,
                    POSTGRES_KIND, USERNAME_PWD, USERNAME_PWD, SCHEMA_MANAGEMENT_STRATEGY));

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
