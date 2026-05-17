package io.quarkus.hibernate.reactive.compatibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when only reactive is enabled on the default datasource.
 * The JDBC driver is not included, so blocking (JDBC/Agroal) is unavailable.
 *
 * @see CompatibilityTestScenario#DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER
 */
public class DefaultReactiveOnlyCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER
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
