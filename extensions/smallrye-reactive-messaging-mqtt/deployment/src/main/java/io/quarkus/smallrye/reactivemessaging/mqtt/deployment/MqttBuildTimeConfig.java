package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mqtt", phase = ConfigPhase.BUILD_TIME)
public class MqttBuildTimeConfig {

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a MQTT broker in dev and test mode.
     */
    @ConfigItem
    public MqttDevServicesBuildTimeConfig devservices;
}
