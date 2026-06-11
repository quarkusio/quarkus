package io.quarkus.bootstrap.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class DependencyUtilsTest {

    @Test
    void getKeyPreservesTestJarType() {
        var artifact = new DefaultArtifact("org.acme", "acme-lib", "tests-quarkus", "jar", "1.0",
                Map.of(ArtifactProperties.TYPE, "test-jar"), (org.eclipse.aether.artifact.ArtifactType) null);
        ArtifactKey key = DependencyUtils.getKey(artifact);
        assertThat(key.getType()).isEqualTo("test-jar");
        assertThat(key.getClassifier()).isEqualTo("tests-quarkus");
    }

    @Test
    void getKeyDefaultsToExtensionWhenNoTypeProperty() {
        var artifact = new DefaultArtifact("org.acme", "acme-lib", "", "jar", "1.0");
        ArtifactKey key = DependencyUtils.getKey(artifact);
        assertThat(key.getType()).isEqualTo("jar");
    }

    @Test
    void getCoordsPreservesTestJarType() {
        var artifact = new DefaultArtifact("org.acme", "acme-lib", "tests-quarkus", "jar", "1.0",
                Map.of(ArtifactProperties.TYPE, "test-jar"), (org.eclipse.aether.artifact.ArtifactType) null);
        ArtifactCoords coords = DependencyUtils.getCoords(artifact);
        assertThat(coords.getType()).isEqualTo("test-jar");
        assertThat(coords.getClassifier()).isEqualTo("tests-quarkus");
        assertThat(coords.getVersion()).isEqualTo("1.0");
    }

    @Test
    void toAppArtifactPreservesTestJarType() {
        var artifact = new DefaultArtifact("org.acme", "acme-lib", "tests-quarkus", "jar", "1.0",
                Map.of(ArtifactProperties.TYPE, "test-jar"), (org.eclipse.aether.artifact.ArtifactType) null);
        var resolved = DependencyUtils.toAppArtifact(artifact, null).build();
        assertThat(resolved.getType()).isEqualTo("test-jar");
        assertThat(resolved.getClassifier()).isEqualTo("tests-quarkus");
    }
}
