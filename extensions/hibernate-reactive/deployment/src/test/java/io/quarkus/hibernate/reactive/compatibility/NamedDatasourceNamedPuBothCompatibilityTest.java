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
 * Tests compatibility when a named datasource ("named-datasource") serves a named persistence unit
 * ("named-pu"), providing both blocking and reactive access through the same PU.
 *
 * @see CompatibilityTestScenario#NAMED_DATASOURCE_NAMED_PU_BOTH
 */
public class NamedDatasourceNamedPuBothCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.NAMED_DATASOURCE_NAMED_PU_BOTH
            .configure(CompatibilityTestScenario.baseTest());

    @PersistenceUnit("named-pu")
    Mutiny.SessionFactory namedMutinySessionFactory;

    @Inject
    @PersistenceUnit("named-pu")
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
