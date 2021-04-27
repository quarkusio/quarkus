package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-messaging.kafka", phase = ConfigPhase.BUILD_TIME)
public class ReactiveMessagingKafkaBuildTimeConfig {
    /**
     * Whether or not Kafka serializer/deserializer autodetection is enabled.
     */
    @ConfigItem(name = "serializer-autodetection.enabled", defaultValue = "true")
    public boolean serializerAutodetectionEnabled;
}
