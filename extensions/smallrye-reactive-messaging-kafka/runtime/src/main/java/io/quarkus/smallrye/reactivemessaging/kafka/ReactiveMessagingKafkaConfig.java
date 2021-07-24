package io.quarkus.smallrye.reactivemessaging.kafka;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-messaging.kafka")
public class ReactiveMessagingKafkaConfig {

    /**
     * Enables the graceful shutdown in dev and test modes.
     * The graceful shutdown waits until the inflight records have been processed and the offset committed to Kafka.
     * While this setting is highly recommended in production, in dev and test modes, it's disabled by default.
     * This setting allows to re-enable it.
     */
    @ConfigProperty(defaultValue = "false")
    public boolean enableGracefulShutdownInDevAndTestMode;

}
