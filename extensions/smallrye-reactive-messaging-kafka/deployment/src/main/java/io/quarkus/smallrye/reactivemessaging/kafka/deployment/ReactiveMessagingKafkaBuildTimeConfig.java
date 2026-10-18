package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.messaging.kafka")
public interface ReactiveMessagingKafkaBuildTimeConfig {
    /**
     * Whether or not Kafka serializer/deserializer auto-detection is enabled.
     */
    @WithName("serializer-autodetection.enabled")
    @WithDefault("true")
    boolean serializerAutodetectionEnabled();

    /**
     * Whether Kafka serializer/deserializer generation is enabled.
     * When no serializer/deserializer are found and not set, Quarkus generates a Jackson-based serde.
     */
    @WithName("serializer-generation.enabled")
    @WithDefault("true")
    boolean serializerGenerationEnabled();

    /**
     * Enables the graceful shutdown in dev and test modes.
     * The graceful shutdown waits until the inflight records have been processed and the offset committed to Kafka.
     * While this setting is highly recommended in production, in dev and test modes, it's disabled by default.
     * This setting allows to re-enable it.
     */
    @WithDefault("false")
    boolean enableGracefulShutdownInDevAndTestMode();
}
