package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.runtime.QuarkusRecoveryService;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that the recovery service is NOT started when no XA datasources
 * are configured and {@code enable-recovery} is not explicitly set.
 */
public class TransactionRecoveryNotEnabledWithoutXaTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void testRecoveryNotStartedWithoutXaDatasources() {
        assertFalse(QuarkusRecoveryService.isRunning(),
                "Recovery service should not be running when no XA datasources are configured");
    }
}
