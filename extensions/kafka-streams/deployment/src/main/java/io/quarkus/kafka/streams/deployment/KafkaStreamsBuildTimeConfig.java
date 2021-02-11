package io.quarkus.kafka.streams.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kafka-streams", phase = ConfigPhase.BUILD_TIME)
public class KafkaStreamsBuildTimeConfig {

    /**
     * Whether or not a health check is published in case the smallrye-health extension is present (defaults to true).
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;
}
