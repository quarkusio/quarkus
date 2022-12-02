package io.quarkus.bootstrap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AppArtifactCoordsTest {

    @Test
    void testFails() {
        String message = assertThrows(IllegalArgumentException.class, () -> AppArtifactCoords.fromString("test-artifact"))
                .getMessage();
        Assertions.assertTrue(message.contains("Invalid AppArtifactCoords string without any separator"));
    }

    @Test
    void testGAFails() {
        String message = assertThrows(IllegalArgumentException.class,
                () -> AppArtifactCoords.fromString("io.quarkus:test-artifact")).getMessage();
        Assertions.assertTrue(message.contains("Use AppArtifactKey instead of AppArtifactCoords"));
    }

    @Test
    void testGAV() {
        final AppArtifactCoords appArtifactCoords = AppArtifactCoords.fromString("io.quarkus:test-artifact:1.1");
        assertEquals("io.quarkus", appArtifactCoords.getGroupId());
        assertEquals("test-artifact", appArtifactCoords.getArtifactId());
        assertEquals("1.1", appArtifactCoords.getVersion());
        assertEquals("", appArtifactCoords.getClassifier());
        assertEquals("jar", appArtifactCoords.getType());
    }

    @Test
    void testGACV() {
        final AppArtifactCoords appArtifactCoords = AppArtifactCoords.fromString("io.quarkus:test-artifact:classif:1.1");
        assertEquals("io.quarkus", appArtifactCoords.getGroupId());
        assertEquals("test-artifact", appArtifactCoords.getArtifactId());
        assertEquals("1.1", appArtifactCoords.getVersion());
        assertEquals("classif", appArtifactCoords.getClassifier());
        assertEquals("jar", appArtifactCoords.getType());
    }

    @Test
    void testGACTV() {
        final AppArtifactCoords appArtifactCoords = AppArtifactCoords.fromString("io.quarkus:test-artifact:classif:json:1.1");
        assertEquals("io.quarkus", appArtifactCoords.getGroupId());
        assertEquals("test-artifact", appArtifactCoords.getArtifactId());
        assertEquals("1.1", appArtifactCoords.getVersion());
        assertEquals("classif", appArtifactCoords.getClassifier());
        assertEquals("json", appArtifactCoords.getType());
    }
}
