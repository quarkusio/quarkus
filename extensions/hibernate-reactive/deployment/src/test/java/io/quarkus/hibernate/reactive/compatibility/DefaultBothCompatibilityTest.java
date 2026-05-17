package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests compatibility when both blocking (JDBC/Agroal) and reactive (Vert.x pool) are enabled
 * on the default datasource, using the default persistence unit.
 * <p>
 * Also asserts that schema migration runs only once even with two session factories.
 *
 * @see CompatibilityTestScenario#DEFAULT_BOTH
 */
public class DefaultBothCompatibilityTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = CompatibilityTestScenario.DEFAULT_BOTH
            .configure(CompatibilityTestScenario.baseTest())
            .setLogRecordPredicate(record -> "org.hibernate.SQL".equals(record.getLoggerName()))
            .assertLogRecords(
                    records -> // When using both blocking and reactive we don't want migration to be applied twice
                    assertThat(records.stream().map(l -> l.getMessage()))
                            .containsOnlyOnce("create sequence hero_SEQ start with 1 increment by 50"));

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        testReactiveWorks(asserter);
    }

    @Test
    public void testBlocking() {
        testBlockingWorks();
    }
}
