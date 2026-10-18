package io.quarkus.extest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.ProdModeTestResults;

/**
 * Base test class that verifies when a {@link io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem}
 * is produced during build, the generated JAR manifest contains the {@code Enable-Native-Access: ALL-UNNAMED} entry.
 * Subclasses configure the specific packaging type (uber-jar, fast-jar) and provide the test results.
 */
abstract class AbstractModuleEnableNativeAccessManifestTest {

    abstract ProdModeTestResults prodModeTestResults();

    @Test
    void testEnableNativeAccessManifestEntry() throws Exception {
        ProdModeTestResults results = prodModeTestResults();
        assertThat(results.getResults()).hasSize(1);
        Path jarPath = results.getResults().get(0).getPath();

        try (InputStream fileInputStream = new FileInputStream(jarPath.toFile());
                JarInputStream stream = new JarInputStream(fileInputStream)) {
            Manifest manifest = stream.getManifest();
            assertThat(manifest).isNotNull();

            String enableNativeAccess = manifest.getMainAttributes().getValue("Enable-Native-Access");
            assertThat(enableNativeAccess)
                    .as("Manifest should contain Enable-Native-Access entry when ModuleEnableNativeAccessBuildItem is produced")
                    .isNotNull()
                    .isEqualTo("ALL-UNNAMED");
        }
    }
}
