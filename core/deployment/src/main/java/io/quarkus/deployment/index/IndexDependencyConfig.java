package io.quarkus.deployment.index;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

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

}
