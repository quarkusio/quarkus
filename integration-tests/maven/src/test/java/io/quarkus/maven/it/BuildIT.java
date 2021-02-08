package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
public class BuildIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testMultiModuleAppRootWithNoSources()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/multimodule-root-no-src", "projects/multimodule-root-no-src-build");

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("install -pl .,html,rest"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        result = running.execute(Collections.singletonList("quarkus:build -f runner"), Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = new File(testDir, "runner" + File.separator + "target");
        final File runnerJar = targetDir.toPath().resolve("quarkus-app").resolve("quarkus-run.jar").toFile();

        // make sure the jar can be read by JarInputStream
        ensureManifestOfJarIsReadableByJarInputStream(runnerJar);
    }

    private void ensureManifestOfJarIsReadableByJarInputStream(File jar) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(jar)) {
            try (JarInputStream stream = new JarInputStream(fileInputStream)) {
                Manifest manifest = stream.getManifest();
                assertThat(manifest).isNotNull();
            }
        }
    }
}
