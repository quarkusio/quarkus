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

    public static String ensureCatalogArtifactId(String artifactId) {
        return isCatalogArtifactId(artifactId) ? artifactId : artifactId + Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX;
    }

    public static boolean isCatalogArtifactId(String artifactId) {
        return artifactId != null && artifactId.endsWith(Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX);
    }

    public static boolean isCatalogArtifact(ArtifactCoords coords) {
        return isCatalogArtifactId(coords.getArtifactId());
    }

    public static String ensureBomArtifactId(String artifactId) {
        return isCatalogArtifactId(artifactId)
                ? artifactId.substring(0, artifactId.length() - Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length())
                : artifactId;
    }

    public static ArtifactCoords ensureBomArtifact(ArtifactCoords coords) {
        return isCatalogArtifactId(coords.getArtifactId())
                ? getBomArtifactForCatalog(coords)
                : coords;
    }

    public static ArtifactCoords getBomArtifactForCatalog(ArtifactCoords coords) {
        return new ArtifactCoords(coords.getGroupId(),
                coords.getArtifactId().substring(0,
                        coords.getArtifactId().length() - Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length()),
                null, "pom", coords.getVersion());
    }

}
