package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@EnableForNative
public class NativeAgentIT extends MojoTestBase {

    @Test
    public void testRunIntegrationTests() throws MavenInvocationException, IOException, InterruptedException {
        final File testDir = initProject("projects/native-agent-integration");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult runJvmITsWithAgent = running.execute(
                List.of("clean", "verify", "-DskipITs=false", "-Dquarkus.test.integration-test-profile=test-with-native-agent"),
                Map.of());
        assertThat(runJvmITsWithAgent.getProcess().waitFor()).isZero();

        MavenProcessInvocationResult runNativeITs = running
                .execute(nativeArguments("verify", "-Dnative", "-Dquarkus.native.agent-configuration-apply"), Map.of());
        assertThat(runNativeITs.getProcess().waitFor()).isZero();
    }

    private List<String> nativeArguments(String... initialArguments) {
        final List<String> result = new ArrayList<>(Arrays.asList(initialArguments));
        appendArgumentIfSet("quarkus.native.container-build", result);
        appendArgumentIfSet("quarkus.native.builder-image", result);
        return result;
    }

    private static void appendArgumentIfSet(String property, List<String> result) {
        final String value = System.getProperty(property);
        if (value != null) {
            if (value.isEmpty()) {
                result.add("-D" + property);
            } else {
                result.add(String.format("-D%s=%s", property, value));
            }
        }
    }
}