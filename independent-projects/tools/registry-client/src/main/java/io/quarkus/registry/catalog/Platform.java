package io.quarkus.registry.catalog;

import io.quarkus.maven.ArtifactCoords;

public interface Platform {

    ArtifactCoords getBom();

    String getQuarkusCoreVersion();

    String getUpstreamQuarkusCoreVersion();
}
