package io.quarkus.deployment.index;

import java.util.Optional;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface IndexDependencyConfig {

    /**
     * The maven groupId of the artifact.
     */
    String groupId();

    /**
     * The maven artifactId of the artifact (optional).
     */
    Optional<String> artifactId();

    /**
     * The maven classifier of the artifact (optional).
     */
    Optional<String> classifier();

    /**
     * The maven type of the artifact.
     */
    @WithDefault(ArtifactCoords.TYPE_JAR)
    String type();

}
