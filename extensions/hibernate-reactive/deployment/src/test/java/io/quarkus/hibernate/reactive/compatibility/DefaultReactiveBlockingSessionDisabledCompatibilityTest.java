package io.quarkus.hibernate.reactive.compatibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when the JDBC driver is present but the blocking session factory
 * is explicitly disabled via {@code quarkus.hibernate-orm.blocking=false}.
 * The default datasource is reactive only.
 *
 * @see CompatibilityTestScenario#DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED
 */
public class DefaultReactiveBlockingSessionDisabledCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED
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
