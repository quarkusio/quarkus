package io.quarkus.registry.catalog;

import io.quarkus.maven.ArtifactCoords;
import java.util.List;
import java.util.Map;

public interface Extension {

    String MD_SHORT_NAME = "short-name";
    String MD_NESTED_CODESTART_NAME = "codestart.name";
    String MD_NESTED_CODESTART_LANGUAGES = "codestart.languages";
    String MD_NESTED_CODESTART_KIND = "codestart.kind";
    String MD_NESTED_CODESTART_ARTIFACT = "codestart.artifact";

    String MD_GUIDE = "guide";
    String MD_KEYWORDS = "keywords";
    String MD_UNLISTED = "unlisted";
    String MD_CATEGORIES = "categories";
    String MD_STATUS = "status";
    String MD_BUILT_WITH_QUARKUS_CORE = "built-with-quarkus-core";

    String getName();

    String getDescription();

    ArtifactCoords getArtifact();

    List<ExtensionOrigin> getOrigins();

    default boolean hasPlatformOrigin() {
        final List<ExtensionOrigin> origins = getOrigins();
        if (origins == null || origins.isEmpty()) {
            return false;
        }
        for (ExtensionOrigin o : origins) {
            if (o.isPlatform()) {
                return true;
            }
        }
        return false;
    }

    Map<String, Object> getMetadata();

    default String managementKey() {
        final ArtifactCoords artifact = getArtifact();
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

}
