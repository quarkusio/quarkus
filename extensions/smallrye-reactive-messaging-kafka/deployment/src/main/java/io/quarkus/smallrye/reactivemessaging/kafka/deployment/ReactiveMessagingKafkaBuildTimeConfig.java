package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "messaging.kafka", phase = ConfigPhase.BUILD_TIME)
public class ReactiveMessagingKafkaBuildTimeConfig {
    /**
     * Whether or not Kafka serializer/deserializer auto-detection is enabled.
     */
    @ConfigItem(name = "serializer-autodetection.enabled", defaultValue = "true")
    public boolean serializerAutodetectionEnabled;

    /**
     * Whether Kafka serializer/deserializer generation is enabled.
     * When no serializer/deserializer are found and not set, Quarkus generates a Jackson-based serde.
     */
    @ConfigItem(name = "serializer-generation.enabled", defaultValue = "true")
    public boolean serializerGenerationEnabled;
}
