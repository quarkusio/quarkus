package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
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
        MavenProcessInvocationResult result = running.execute(List.of("clean", "verify", "-Pnative-with-agent"), Map.of());
        assertThat(result.getProcess().waitFor()).isZero();
    }
}
