package io.quarkus.registry.util;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;

public class PlatformArtifacts {

    public static ArtifactCoords getCatalogArtifactForBom(ArtifactCoords bom) {
        return getCatalogArtifactForBom(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
    }

    public static ArtifactCoords getCatalogArtifactForBom(String bomGroupId, String bomArtifactId, String bomVersion) {
        return new ArtifactCoords(bomGroupId, bomArtifactId + Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX, bomVersion,
                "json", bomVersion);
    }

    public static ArtifactCoords ensureCatalogArtifact(ArtifactCoords coords) {
        return isCatalogArtifact(coords) ? coords
                : getCatalogArtifactForBom(coords.getGroupId(), coords.getArtifactId(), coords.getVersion());
    }

    public static boolean isCatalogArtifactId(String artifactId) {
        return artifactId != null && artifactId.endsWith(Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX);
    }

    public static boolean isCatalogArtifact(ArtifactCoords coords) {
        return isCatalogArtifactId(coords.getArtifactId());
    }
}
