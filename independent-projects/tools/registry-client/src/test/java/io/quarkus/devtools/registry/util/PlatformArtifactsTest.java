package io.quarkus.devtools.registry.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.util.PlatformArtifacts;
import org.junit.jupiter.api.Test;

public class PlatformArtifactsTest {

    @Test
    public void getCatalogArtifactForBom() {
        final ArtifactCoords catalog = new ArtifactCoords("org.acme", "acme-bom-quarkus-platform-descriptor", "1.0", "json",
                "1.0");
        assertEquals(catalog,
                PlatformArtifacts.getCatalogArtifactForBom(new ArtifactCoords("org.acme", "acme-bom", null, "pom", "1.0")));
        assertEquals(catalog, PlatformArtifacts.getCatalogArtifactForBom("org.acme", "acme-bom", "1.0"));
    }

    @Test
    public void ensureCatalogArtifact() {
        final ArtifactCoords catalog = new ArtifactCoords("org.acme", "acme-bom-quarkus-platform-descriptor", "1.0", "json",
                "1.0");
        assertEquals(catalog, PlatformArtifacts.ensureCatalogArtifact(catalog));
        assertEquals(catalog,
                PlatformArtifacts.ensureCatalogArtifact(new ArtifactCoords("org.acme", "acme-bom", null, "pom", "1.0")));
    }

    @Test
    public void isCatalogArtifactId() {
        assertTrue(PlatformArtifacts.isCatalogArtifactId("acme-bom-quarkus-platform-descriptor"));
        assertFalse(PlatformArtifacts.isCatalogArtifactId("acme-bom"));
    }

    @Test
    public void isCatalogArtifact() {
        assertTrue(PlatformArtifacts.isCatalogArtifact(
                new ArtifactCoords("org.acme", "acme-bom-quarkus-platform-descriptor", "1.0", "json", "1.0")));
        assertFalse(PlatformArtifacts.isCatalogArtifact(new ArtifactCoords("org.acme", "acme-bom", null, "pom", "1.0")));
    }
}
