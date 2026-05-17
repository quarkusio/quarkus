package io.quarkus.hibernate.reactive.compatibility;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when a named datasource ("named-datasource") provides reactive-only access
 * for a named persistence unit ("named-pu"), while the default datasource provides blocking-only
 * access for the default persistence unit.
 *
 * @see CompatibilityTestScenario#NAMED_REACTIVE_DEFAULT_BLOCKING
 */
public class NamedReactiveDatasourceDefaultBlockingCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.NAMED_REACTIVE_DEFAULT_BLOCKING
            .configure(CompatibilityTestScenario.baseTest());

    @PersistenceUnit("named-pu")
    Mutiny.SessionFactory namedMutinySessionFactory;

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter uniAsserter) {
        testReactiveWorks(namedMutinySessionFactory, uniAsserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
