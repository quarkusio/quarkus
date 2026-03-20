package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.runtime.QuarkusRecoveryService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that explicitly setting {@code enable-recovery=true} starts the recovery
 * service even when no XA datasources are configured.
 */
public class TransactionRecoveryExplicitlyEnabledWithoutXaTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.transaction-manager.enable-recovery", "true");

    @Test
    public void testRecoveryStartedWhenExplicitlyEnabled() {
        assertTrue(QuarkusRecoveryService.isRunning(),
                "Recovery service should be running when explicitly enabled, even without XA datasources");
    }
}
