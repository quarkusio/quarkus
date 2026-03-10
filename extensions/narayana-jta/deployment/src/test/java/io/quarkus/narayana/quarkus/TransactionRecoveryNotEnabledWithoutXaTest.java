package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that the recovery service is NOT started when no XA datasources
 * are configured and {@code enable-recovery} is not explicitly set.
 */
public class TransactionRecoveryNotEnabledWithoutXaTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void testRecoveryNotStartedWithoutXaDatasources() {
        assertFalse(Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.getName().equals("Periodic Recovery")),
                "Periodic Recovery thread should not be running when no XA datasources are configured");
    }
}
