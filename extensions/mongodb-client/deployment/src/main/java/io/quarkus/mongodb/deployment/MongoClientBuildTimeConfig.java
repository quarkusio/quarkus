package io.quarkus.mongodb.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mongodb", phase = ConfigPhase.BUILD_TIME)
public class MongoClientBuildTimeConfig {
    /**
     * Whether or not an healtcheck is published in case the smallrye-health extension is present (default to true).
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;
}
