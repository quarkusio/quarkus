package io.quarkus.hibernate.reactive.compatibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when the JDBC driver is present but JDBC is explicitly disabled
 * via {@code quarkus.datasource.jdbc=false}. The default datasource is reactive only.
 *
 * @see CompatibilityTestScenario#DEFAULT_REACTIVE_JDBC_DISABLED
 */
public class DefaultReactiveJdbcDisabledCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.DEFAULT_REACTIVE_JDBC_DISABLED
            .configure(CompatibilityTestScenario.baseTest());

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        testReactiveWorks(asserter);
    }

    @Test
    public void testBlocking() {
        testBlockingDisabled();
    }
}
