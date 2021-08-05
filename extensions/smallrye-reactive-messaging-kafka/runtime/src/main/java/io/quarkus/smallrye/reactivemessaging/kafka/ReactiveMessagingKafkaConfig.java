package io.quarkus.smallrye.reactivemessaging.kafka;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-messaging.kafka")
public class ReactiveMessagingKafkaConfig {

    /**
     * Enables the graceful shutdown in dev and test modes.
     * The graceful shutdown waits until the inflight records have been processed and the offset committed to Kafka.
     * While this setting is highly recommended in production, in dev and test modes, it's disabled by default.
     * This setting allows to re-enable it.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableGracefulShutdownInDevAndTestMode;

}
