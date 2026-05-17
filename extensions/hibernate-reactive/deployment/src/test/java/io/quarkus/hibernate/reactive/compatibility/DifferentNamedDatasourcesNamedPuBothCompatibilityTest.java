package io.quarkus.hibernate.reactive.compatibility;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when two entirely separate named datasources are used:
 * "named-datasource-reactive" (reactive-only) backing "named-pu-reactive", and
 * "named-datasource-blocking" (JDBC-only) backing "named-pu-blocking".
 *
 * @see CompatibilityTestScenario#DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH
 */
public class DifferentNamedDatasourcesNamedPuBothCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH
            .configure(CompatibilityTestScenario.baseTest());

    @PersistenceUnit("named-pu-reactive")
    Mutiny.SessionFactory namedMutinySessionFactory;

    @Inject
    @PersistenceUnit("named-pu-blocking")
    SessionFactory namedPersistenceUnitSessionFactory;

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter uniAsserter) {
        testReactiveWorks(namedMutinySessionFactory, uniAsserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks(namedPersistenceUnitSessionFactory);
    }
}
