package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.sqlclient.Pool;

public class ORMReactiveCompatbilityNamedDataSourceReactiveUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .withConfiguration("""
                    quarkus.hibernate-orm.schema-management.strategy=%s
                    quarkus.hibernate-orm.datasource=named-datasource
                    quarkus.datasource."named-datasource".reactive=true
                    quarkus.datasource."named-datasource".db-kind=%s
                    quarkus.datasource."named-datasource".username=%s
                    quarkus.datasource."named-datasource".password=%s
                    """.formatted(SCHEMA_MANAGEMENT_STRATEGY, POSTGRES_KIND, USERNAME_PWD));

    @Inject
    @ReactiveDataSource("named-datasource")
    Pool pool;

    @Test
    @RunOnVertxContext
    public void test(UniAsserter uniAsserter) {
        testReactiveWorks(uniAsserter);
        assertThat(pool).isNotNull();
    }
}
