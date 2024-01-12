package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
public class ImageBuildIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    // We can only test with jib as it's the only extension that has 0 dependencies from the system.
    @Test
    @EnabledOnOs({ OS.LINUX })
    public void testImageBuildWithJib() throws Exception {
        Properties buildProperties = new Properties();
        buildProperties.put("quarkus.container-image.builder", "jib");

        testDir = initProject("projects/classic", "projects/image-build-with-jib");
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("quarkus:image-build"),
                Collections.emptyMap(), buildProperties);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }

}
