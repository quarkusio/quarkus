package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link JarResultBuildStep}
 */
class JarResultBuildStepTest {

    @Test
    void should_unsign_jar_when_filtered(@TempDir Path tempDir) throws Exception {
        Path signedJarFilePath = Path.of(getClass().getClassLoader().getResource("signed.jar").toURI());
        Path jarFilePath = tempDir.resolve("unsigned.jar");
        JarResultBuildStep.filterJarFile(signedJarFilePath, jarFilePath,
                Set.of("org/eclipse/jgit/transport/sshd/SshdSessionFactory.class"));
        try (JarFile jarFile = new JarFile(jarFilePath.toFile())) {
            assertThat(jarFile.stream().map(JarEntry::getName)).doesNotContain("META-INF/ECLIPSE_.RSA", "META-INF/ECLIPSE_.SF");
            // Check that the manifest is still present
            Manifest manifest = jarFile.getManifest();
            assertThat(manifest.getMainAttributes()).isNotEmpty();
            assertThat(manifest.getEntries()).isEmpty();
        }
    }

}
