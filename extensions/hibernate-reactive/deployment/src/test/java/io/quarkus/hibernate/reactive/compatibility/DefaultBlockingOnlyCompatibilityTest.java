package io.quarkus.hibernate.reactive.compatibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;

/**
 * Tests compatibility when blocking (JDBC/Agroal) is enabled and reactive is explicitly disabled
 * on the default datasource, using the default persistence unit.
 *
 * @see CompatibilityTestScenario#DEFAULT_BLOCKING_ONLY
 */
public class DefaultBlockingOnlyCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.DEFAULT_BLOCKING_ONLY
            .configure(CompatibilityTestScenario.baseTest());

    @Test
    @RunOnVertxContext
    public void testReactive() {
        testReactiveDisabled();
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
