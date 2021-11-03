package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rabbitmq", phase = ConfigPhase.BUILD_TIME)
public class RabbitMQBuildTimeConfig {

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a RabbitMQ broker in dev and test mode.
     */
    @ConfigItem
    public RabbitMQDevServicesBuildTimeConfig devservices;
}
