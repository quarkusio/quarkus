package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class CustomManifestSectionsThinJarTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setApplicationName("Custom-Manifest-Thin")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("projects/custom-manifest-section/custom-manifest-thin.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testManifestEntries() throws Exception {
        assertThat(prodModeTestResults.getResults()).hasSize(1);
        Path jarPath = prodModeTestResults.getResults().get(0).getPath();

        try (InputStream fileInputStream = new FileInputStream(jarPath.toFile())) {
            try (JarInputStream stream = new JarInputStream(fileInputStream)) {
                Manifest manifest = stream.getManifest();
                assertThat(manifest).isNotNull();
                assertThat(manifest.getEntries().size()).isEqualTo(1);
                Attributes testAttributes = manifest.getEntries().get("Test-Information");
                assertThat(testAttributes).isNotNull();
                Attributes.Name testKey1 = new Attributes.Name("Test-Key-1");
                Assertions.assertTrue(testAttributes.containsKey(testKey1), "Custom Manifest Entry for Test-Key-1 is missing");
                Assertions.assertEquals("Test Value 1", testAttributes.getValue(testKey1),
                        "Custom Manifest Entry for Test-Key-1 value is not correct");
                Assertions.assertTrue(testAttributes.containsKey(new Attributes.Name("Test-Key-2")),
                        "Custom Manifest Entry for Test-Key-2 is missing");
            }
        }
    }
}
