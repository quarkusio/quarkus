package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.narayana.jta.runtime.QuarkusRecoveryService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that the recovery service is automatically enabled when XA datasources
 * are configured and {@code enable-recovery} is not explicitly set.
 */
public class TransactionRecoveryXaAutoEnableTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("xa-recovery-auto-enable.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-agroal", Version.getVersion())));

    @Test
    public void testRecoveryAutoEnabledWithXaDatasource() {
        assertTrue(QuarkusRecoveryService.isRunning(),
                "Recovery service should be running when XA datasources are configured");
    }
}
