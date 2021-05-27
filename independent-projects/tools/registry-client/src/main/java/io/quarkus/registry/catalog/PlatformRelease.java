package io.quarkus.registry.catalog;

import io.quarkus.maven.ArtifactCoords;
import java.util.Collection;
import java.util.Map;

/**
 * Platform release
 */
public interface PlatformRelease {

    PlatformReleaseVersion getVersion();

    Collection<ArtifactCoords> getMemberBoms();

    String getQuarkusCoreVersion();

    String getUpstreamQuarkusCoreVersion();

    Map<String, Object> getMetadata();
}
