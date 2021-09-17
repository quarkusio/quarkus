package io.quarkus.neo4j.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServicesBuildTimeConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a database when running in Dev or Test mode.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * The container image name to use, for container based DevServices providers.
     */
    @ConfigItem(defaultValue = "neo4j:4.3")
    public String imageName;

    /**
     * Additional environment entries that can be added to the container before its start.
     */
    @ConfigItem
    public Map<String, String> additionalEnv;
}
