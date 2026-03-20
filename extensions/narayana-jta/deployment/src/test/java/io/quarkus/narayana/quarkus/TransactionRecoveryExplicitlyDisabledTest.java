package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.narayana.jta.runtime.QuarkusRecoveryService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that explicitly setting {@code enable-recovery=false} prevents the recovery
 * service from starting, even when XA datasources are configured.
 */
public class TransactionRecoveryExplicitlyDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("xa-recovery-auto-enable.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-agroal", Version.getVersion())))
            .overrideConfigKey("quarkus.transaction-manager.enable-recovery", "false");

    @Test
    public void testRecoveryNotStartedWhenExplicitlyDisabled() {
        assertFalse(QuarkusRecoveryService.isRunning(),
                "Recovery service should not be running when recovery is explicitly disabled");
    }
}
