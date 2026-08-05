package io.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.ResourceReaper;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/pull/28702">#28702</a>:
 * checking Docker availability must not start Ryuk, and must not persist
 * {@code testcontainers.reuse.enable} either in-memory
 * (in {@link TestcontainersConfiguration#getUserProperties()})
 * or on disk ({@code ~/.testcontainers.properties}).
 * <p>
 * Note: Testcontainers itself may create {@code ~/.testcontainers.properties}
 * to persist the {@code docker.client.strategy} — that is normal and harmless.
 * What matters is that our Docker check does not add {@code testcontainers.reuse.enable}
 * to that file, and does not start Ryuk.
 */
class IsContainerRuntimeWorkingTest {

    private static final File TESTCONTAINERS_PROPS_FILE = new File(System.getProperty("user.home"),
            ".testcontainers.properties");

    /**
     * Regression test for <a href="https://github.com/quarkusio/quarkus/issues/25852">#25852</a>:
     * checking Docker availability must not start Ryuk.
     * <p>
     * {@code getTransportConfig()} checks Docker availability without calling
     * {@code client()}, so it must not initialize {@link ResourceReaper}
     * (which starts Ryuk when reuse is not enabled).
     */
    @Test
    void dockerAvailabilityCheckDoesNotStartRyuk() throws Exception {
        Field instanceField = ResourceReaper.class.getDeclaredField("instance");
        instanceField.setAccessible(true);

        Object reaperBefore = instanceField.get(null);

        new IsContainerRuntimeWorking.TestContainersStrategy(true).get();

        Object reaperAfter = instanceField.get(null);
        if (reaperBefore == null) {
            assertThat(reaperAfter)
                    .as("Docker availability check must not initialize ResourceReaper"
                            + " (getTransportConfig() must not call client())")
                    .isNull();
        } else if (reaperAfter.getClass().getSimpleName().equals("RyukResourceReaper")) {
            Field startedField = reaperAfter.getClass().getDeclaredField("started");
            startedField.setAccessible(true);
            AtomicBoolean started = (AtomicBoolean) startedField.get(reaperAfter);
            assertThat(started.get())
                    .as("Docker availability check must not start Ryuk")
                    .isFalse();
        }
    }

    @Test
    void dockerAvailabilityCheckDoesNotLeaveConfigBehind() throws Exception {
        TestcontainersConfiguration config = TestcontainersConfiguration.getInstance();
        Properties userProperties = config.getUserProperties();

        String originalPropertyValue = userProperties.getProperty("testcontainers.reuse.enable");
        assumeTrue(originalPropertyValue == null || !"true".equals(originalPropertyValue),
                "testcontainers.reuse.enable is already set to true;"
                        + " the test cannot verify that the Docker check does not enable it");

        new IsContainerRuntimeWorking.TestContainersStrategy(true).get();

        assertThat(userProperties.getProperty("testcontainers.reuse.enable"))
                .as("testcontainers.reuse.enable in-memory property should be restored after Docker check")
                .isEqualTo(originalPropertyValue);

        // Testcontainers may create ~/.testcontainers.properties to persist
        // docker.client.strategy (that's normal). We only care that
        // testcontainers.reuse.enable is not persisted to disk.
        if (TESTCONTAINERS_PROPS_FILE.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(TESTCONTAINERS_PROPS_FILE)) {
                props.load(in);
            }
            assertThat(props.getProperty("testcontainers.reuse.enable"))
                    .as("~/.testcontainers.properties must not contain testcontainers.reuse.enable"
                            + " (IsContainerRuntimeWorking must not persist this setting)")
                    .isNull();
        }
    }
}
