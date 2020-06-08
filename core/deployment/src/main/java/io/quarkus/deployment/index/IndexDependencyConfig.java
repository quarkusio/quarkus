package io.quarkus.deployment.index;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class IndexDependencyConfig {

    /**
     * The maven groupId of the artifact to index
     */
    @ConfigItem
    public String groupId;

    /**
     * The maven artifactId of the artifact to index
     */
    @ConfigItem
    public String artifactId;

    /**
     * The maven classifier of the artifact to index
     */
    @ConfigItem
    public Optional<String> classifier;

}
