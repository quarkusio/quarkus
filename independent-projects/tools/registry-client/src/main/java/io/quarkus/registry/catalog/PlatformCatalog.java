package io.quarkus.registry.catalog;

import io.quarkus.maven.ArtifactCoords;
import java.util.List;

public interface PlatformCatalog {

    List<Platform> getPlatforms();

    ArtifactCoords getDefaultPlatform();
}
