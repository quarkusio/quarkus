package io.quarkus.mongodb.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mongodb", phase = ConfigPhase.BUILD_TIME)
public class MongoClientBuildTimeConfig {
    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Whether or not metrics are published in case a metrics extension is present.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    /**
     * If set to true, the default clients will always be created even if there are no injection points that use them
     */
    @ConfigItem(name = "force-default-clients")
    public boolean forceDefaultClients;
}
