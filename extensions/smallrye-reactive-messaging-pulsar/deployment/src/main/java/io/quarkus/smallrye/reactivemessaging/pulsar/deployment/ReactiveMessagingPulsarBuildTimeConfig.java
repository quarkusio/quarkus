package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "messaging.pulsar", phase = ConfigPhase.BUILD_TIME)
public class ReactiveMessagingPulsarBuildTimeConfig {
    /**
     * Whether or not Pulsar Schema auto-detection is enabled.
     */
    @ConfigItem(name = "schema-autodetection.enabled", defaultValue = "true")
    public boolean schemaAutodetectionEnabled;

    /**
     * Whether Pulsar Schema generation is enabled.
     * When no Schema are found and not set, Quarkus generates a JSON Schema.
     */
    @ConfigItem(name = "schema-generation.enabled", defaultValue = "true")
    public boolean schemaGenerationEnabled;
}
