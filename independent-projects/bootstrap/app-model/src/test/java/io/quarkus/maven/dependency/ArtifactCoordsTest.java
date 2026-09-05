package io.quarkus.maven.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArtifactCoordsTest {

    @Test
    void jarWithoutClassifier() {
        ArtifactCoords coords = ArtifactCoords.jar("org.acme", "acme-lib", "1.0");
        assertThat(coords.toCompactCoords()).isEqualTo("org.acme:acme-lib:1.0");
        assertThat(coords.isJar()).isTrue();
    }

    @Test
    void jarWithClassifier() {
        ArtifactCoords coords = ArtifactCoords.of("org.acme", "acme-lib", "sources", "jar", "1.0");
        assertThat(coords.toCompactCoords()).isEqualTo("org.acme:acme-lib:sources:1.0");
        assertThat(coords.isJar()).isTrue();
    }

    @Test
    void nonJarTypeWithoutClassifier() {
        ArtifactCoords coords = ArtifactCoords.of("org.acme", "acme-lib", "", "pom", "1.0");
        assertThat(coords.toCompactCoords()).isEqualTo("org.acme:acme-lib:pom:1.0");
        assertThat(coords.isJar()).isFalse();
    }

    @Test
    void testJarTypeIsJar() {
        ArtifactCoords coords = ArtifactCoords.of("org.acme", "acme-lib", "tests-quarkus", "test-jar", "1.0");
        assertThat(coords.isJar()).isTrue();
        assertThat(coords.toCompactCoords()).isEqualTo("org.acme:acme-lib:tests-quarkus:test-jar:1.0");
    }
}
