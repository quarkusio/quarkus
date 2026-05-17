package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.sqlclient.Pool;

/**
 * Tests compatibility when only a named reactive datasource ("named-datasource") is configured,
 * with no JDBC driver present. Verifies the reactive Pool is also injectable via CDI.
 *
 * @see CompatibilityTestScenario#NAMED_DATASOURCE_REACTIVE_ONLY
 */
public class NamedDatasourceReactiveOnlyCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.NAMED_DATASOURCE_REACTIVE_ONLY
            .configure(CompatibilityTestScenario.baseTest());

    @Inject
    @ReactiveDataSource("named-datasource")
    Pool pool;

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter uniAsserter) {
        testReactiveWorks(uniAsserter);
        assertThat(pool).isNotNull();
    }
}
