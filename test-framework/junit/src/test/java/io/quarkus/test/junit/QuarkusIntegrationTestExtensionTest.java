package io.quarkus.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.ListeningResults;
import io.quarkus.test.common.TestHostLauncher;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class QuarkusIntegrationTestExtensionTest {

    @Test
    public void tailReturnsLastLinesWithHeaderWhenLogExceedsLimit() {
        List<String> lines = IntStream.rangeClosed(1, 120).mapToObj(i -> "line " + i).toList();

        List<String> tail = QuarkusIntegrationTestExtension.applicationLogTail(lines, 50, "quarkus.log");

        assertThat(tail).hasSize(51); // 1 header + 50 lines
        assertThat(tail.get(0)).isEqualTo("Last 50 line(s) of quarkus.log:");
        assertThat(tail.get(1)).isEqualTo("    line 71");
        assertThat(tail.get(50)).isEqualTo("    line 120");
    }

    @Test
    public void tailReturnsAllLinesWhenLogIsSmallerThanLimit() {
        List<String> lines = List.of("first", "second", "third");

        List<String> tail = QuarkusIntegrationTestExtension.applicationLogTail(lines, 50, "quarkus.log");

        assertThat(tail).containsExactly(
                "Last 3 line(s) of quarkus.log:",
                "    first",
                "    second",
                "    third");
    }

    @Test
    public void tailReturnsEmptyForEmptyLog() {
        assertThat(QuarkusIntegrationTestExtension.applicationLogTail(List.of(), 50, "quarkus.log")).isEmpty();
    }

    @Test
    public void testHostPresentSelectsTestHostLaunchWithoutArtifactMetadata() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(Map.of("quarkus.http.test-host", "1.2.3.4"), "test"))
                .build();

        assertThat(QuarkusIntegrationTestExtension.isTestHostLaunch(config)).isTrue();
    }

    @Test
    public void missingTestHostRequiresArtifactMetadata() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();

        assertThat(QuarkusIntegrationTestExtension.isTestHostLaunch(config)).isFalse();
    }

    @Test
    public void testHostLauncherStartsWithoutArtifactMetadata() throws Exception {
        String prevHost = System.getProperty("quarkus.http.host");
        String prevTestHost = System.getProperty("quarkus.http.test-host");
        String prevTestPort = System.getProperty("quarkus.http.test-port");
        String prevLogPath = System.getProperty("quarkus.test.log.file.path");
        System.setProperty("quarkus.http.test-host", "1.2.3.4");
        System.setProperty("quarkus.http.test-port", "4321");
        System.setProperty("quarkus.test.log.file.path", "target/test-logs/quarkus.log");
        try {
            // No quarkus-artifact.properties is involved here: TestHostLauncher only needs
            // quarkus.http.test-host/test-port, so this is the path taken for an already-running app.
            TestHostLauncher launcher = new TestHostLauncher();
            ListeningResults results = launcher.start();
            try {
                assertThat(results.server()).isPresent();
                assertThat(results.server().get().address().port()).isEqualTo(4321);
                assertThat(results.server().get().address().protocol()).isEqualTo("http");
                assertThat(results.management()).isEmpty();
                assertThat(System.getProperty("quarkus.http.host")).isEqualTo("1.2.3.4");
            } finally {
                launcher.close();
            }
        } finally {
            restoreSystemProperty("quarkus.http.host", prevHost);
            restoreSystemProperty("quarkus.http.test-host", prevTestHost);
            restoreSystemProperty("quarkus.http.test-port", prevTestPort);
            restoreSystemProperty("quarkus.test.log.file.path", prevLogPath);
        }
    }

    private static void restoreSystemProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    @Test
    public void artifactTypeComesFromPropertiesOnNonTestHostPath() {
        Properties properties = new Properties();
        properties.setProperty("type", "jar");

        assertThat(IntegrationTestUtil.getArtifactType(properties)).isEqualTo("jar");
    }

    @Test
    public void missingArtifactTypeFailsOnNonTestHostPath() {
        assertThatThrownBy(() -> IntegrationTestUtil.getArtifactType(new Properties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to determine the type of artifact");
    }

    @Test
    public void clearBootFailureClearsStaticBootFailureState() throws Exception {
        Field failedBoot = QuarkusIntegrationTestExtension.class.getDeclaredField("failedBoot");
        failedBoot.setAccessible(true);
        failedBoot.setBoolean(null, true);
        Field firstException = QuarkusIntegrationTestExtension.class.getDeclaredField("firstException");
        firstException.setAccessible(true);
        firstException.set(null, new RuntimeException("boom"));

        QuarkusIntegrationTestExtension.clearBootFailure();

        assertThat(failedBoot.getBoolean(null)).isFalse();
        assertThat(firstException.get(null)).isNull();
    }
}
