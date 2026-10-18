package io.quarkus.kafka.streams.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.kafka-streams")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KafkaStreamsBuildTimeConfig {

    /**
     * Whether a health check is published in case the smallrye-health extension is present (defaults to true).
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();
}
