package io.quarkus.it.extension.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Be aware! This test will not run if the name does not start with 'Test'.
 */
@Disabled("https://github.com/quarkusio/quarkus/issues/27057")
@DisabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
public class AvroMultimoduleIT extends MojoTestBase {
    @Test
    public void testThatTheTestsPassed() throws MavenInvocationException, InterruptedException {
        File testDir = initProject("projects/avro-multimodule-project", "projects/avro-multimodule-project-build");
        RunningInvoker running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(List.of("clean", "test"), Map.of());
        assertThat(result.getProcess().waitFor()).isZero();
    }
}
