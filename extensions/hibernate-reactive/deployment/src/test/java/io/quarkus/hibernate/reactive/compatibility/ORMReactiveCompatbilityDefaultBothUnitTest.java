package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

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
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .withConfiguration("""
                    quarkus.hibernate-orm.schema-management.strategy=%s
                    quarkus.datasource.reactive=true
                    quarkus.datasource.db-kind=%s
                    quarkus.datasource.username=%s
                    quarkus.datasource.password=%s
                    quarkus.hibernate-orm.log.format-sql=false
                    quarkus.hibernate-orm.log.highlight-sql=false
                    quarkus.log.category."org.hibernate.SQL".level=DEBUG
                    """.formatted(SCHEMA_MANAGEMENT_STRATEGY, POSTGRES_KIND, USERNAME_PWD))
            .setLogRecordPredicate(record -> "org.hibernate.SQL".equals(record.getLoggerName()))
            .assertLogRecords(
                    records -> // When using both blocking and reactive we don't want migration to be applied twice
                    assertThat(records.stream().map(l -> l.getMessage()))
                            .containsOnlyOnce("create sequence hero_SEQ start with 1 increment by 50"));

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
