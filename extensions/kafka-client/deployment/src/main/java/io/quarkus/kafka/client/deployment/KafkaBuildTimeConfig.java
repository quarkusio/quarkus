package io.quarkus.kafka.client.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kafka", phase = ConfigPhase.BUILD_TIME)
public class KafkaBuildTimeConfig {
    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     * <p>
     * If you enable the health check, you must specify the `kafka.bootstrap.servers` property.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "false")
    public boolean healthEnabled;
}
