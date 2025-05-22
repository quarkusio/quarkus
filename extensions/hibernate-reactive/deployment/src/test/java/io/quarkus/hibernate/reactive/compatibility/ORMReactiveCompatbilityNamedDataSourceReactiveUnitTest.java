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
            .withConfigurationResource("application-unittest-onlyreactive-named.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.datasource", "named-datasource")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD);

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
