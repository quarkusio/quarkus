package io.quarkus.hibernate.reactive.compatibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when a named datasource ("named-datasource") serves both blocking
 * and reactive access, using the default persistence unit.
 *
 * @see CompatibilityTestScenario#NAMED_DATASOURCE_BOTH
 */
public class NamedDatasourceBothCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.NAMED_DATASOURCE_BOTH
            .configure(CompatibilityTestScenario.baseTest());

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter uniAsserter) {
        testReactiveWorks(uniAsserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
