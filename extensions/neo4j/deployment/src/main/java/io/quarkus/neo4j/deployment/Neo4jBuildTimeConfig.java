package io.quarkus.neo4j.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "neo4j", phase = ConfigPhase.BUILD_TIME)
public class Neo4jBuildTimeConfig {

    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a Neo4j instance in dev and test mode.
     */
    @ConfigItem
    public DevServicesBuildTimeConfig devservices;
}
