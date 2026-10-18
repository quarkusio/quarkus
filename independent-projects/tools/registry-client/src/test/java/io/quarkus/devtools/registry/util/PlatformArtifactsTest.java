package io.quarkus.devtools.registry.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.util.PlatformArtifacts;

public class PlatformArtifactsTest {

    @Test
    public void getCatalogArtifactForBom() {
        final ArtifactCoords catalog = ArtifactCoords.of("org.acme", "acme-bom-quarkus-platform-descriptor", "1.0", "json",
                "1.0");
        assertEquals(catalog,
                PlatformArtifacts.getCatalogArtifactForBom(ArtifactCoords.of("org.acme", "acme-bom", null, "pom", "1.0")));
        assertEquals(catalog, PlatformArtifacts.getCatalogArtifactForBom("org.acme", "acme-bom", "1.0"));
    }

    @Test
    public void ensureCatalogArtifact() {
        final ArtifactCoords catalog = ArtifactCoords.of("org.acme", "acme-bom-quarkus-platform-descriptor", "1.0", "json",
                "1.0");
        assertEquals(catalog, PlatformArtifacts.ensureCatalogArtifact(catalog));
        assertEquals(catalog,
                PlatformArtifacts.ensureCatalogArtifact(ArtifactCoords.of("org.acme", "acme-bom", null, "pom", "1.0")));
    }

    @Test
    public void ensureCatalogArtifactId() {
        final String catalog = "acme-bom-quarkus-platform-descriptor";
        assertEquals(catalog, PlatformArtifacts.ensureCatalogArtifactId(catalog));
        assertEquals(catalog, PlatformArtifacts.ensureCatalogArtifactId("acme-bom"));
    }

    @Test
    public void isCatalogArtifactId() {
        assertTrue(PlatformArtifacts.isCatalogArtifactId("acme-bom-quarkus-platform-descriptor"));
        assertFalse(PlatformArtifacts.isCatalogArtifactId("acme-bom"));
    }

    @Test
    public void isCatalogArtifact() {
        assertTrue(PlatformArtifacts.isCatalogArtifact(
                ArtifactCoords.of("org.acme", "acme-bom-quarkus-platform-descriptor", "1.0", "json", "1.0")));
        assertFalse(PlatformArtifacts.isCatalogArtifact(ArtifactCoords.of("org.acme", "acme-bom", null, "pom", "1.0")));
    }

    @Test
    public void ensureBomArtifactId() {
        final String bom = "acme-bom";
        assertEquals(bom, PlatformArtifacts.ensureBomArtifactId(bom));
        assertEquals(bom, PlatformArtifacts.ensureBomArtifactId("acme-bom" + Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX));
    }

    @Test
    public void ensureBomArtifact() {
        final ArtifactCoords bom = ArtifactCoords.of("org.acme", "acme-bom", null, "pom", "1.0");
        assertEquals(bom, PlatformArtifacts.ensureBomArtifact(bom));
        assertEquals(bom,
                PlatformArtifacts.ensureBomArtifact(ArtifactCoords.of(bom.getGroupId(),
                        bom.getArtifactId() + Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX, bom.getVersion(), "json",
                        bom.getVersion())));
    }

}
