package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.messaging.pulsar")
public interface ReactiveMessagingPulsarBuildTimeConfig {
    /**
     * Whether or not Pulsar Schema auto-detection is enabled.
     */
    @WithName("schema-autodetection.enabled")
    @WithDefault("true")
    boolean schemaAutodetectionEnabled();

    /**
     * Whether Pulsar Schema generation is enabled.
     * When no Schema are found and not set, Quarkus generates a JSON Schema.
     */
    @WithName("schema-generation.enabled")
    @WithDefault("true")
    boolean schemaGenerationEnabled();
}
