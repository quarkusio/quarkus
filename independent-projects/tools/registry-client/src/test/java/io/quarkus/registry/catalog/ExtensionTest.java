package io.quarkus.registry.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ExtensionTest {

    static Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("src/test/resources/extension");

    @Test
    void deserializeYamlFile() throws Exception {

        final Path extYaml = baseDir.resolve("quarkus-extension.yaml");
        assertThat(extYaml).exists();

        final Extension e = Extension.fromFile(extYaml);
        assertThat(e.getArtifact()).isEqualTo(new ArtifactCoords("io.quarkus", "quarkus-resteasy-reactive", "999-PLACEHOLDER"));
        final Map<String, Object> metadata = e.getMetadata();
        assertThat(metadata.get("short-name")).isEqualTo("resteasy-reactive");
    }
}
