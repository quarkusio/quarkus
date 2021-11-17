package io.quarkus.neo4j.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

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

    /**
     * This value can be used to specify the port to which the bolt-port of the container is exposed. It must be a free
     * port, otherwise startup will fail. A random, free port will be used by default. Either way, a messsage will be
     * logged on which port the Neo4j container is reachable over bolt.
     */
    @ConfigItem
    public OptionalInt boltPort = OptionalInt.empty();

    /**
     * This value can be used to specify the port to which the http-port of the container is exposed. It must be a free
     * port, otherwise startup will fail. A random, free port will be used by default. Either way, a messsage will be
     * logged on which port the Neo4j Browser is available.
     */
    @ConfigItem
    public OptionalInt httpPort = OptionalInt.empty();
}
