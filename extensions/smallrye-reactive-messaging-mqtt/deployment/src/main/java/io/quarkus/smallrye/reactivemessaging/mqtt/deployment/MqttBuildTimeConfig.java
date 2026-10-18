package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.mqtt")
public interface MqttBuildTimeConfig {

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a MQTT broker in dev and test mode.
     */
    MqttDevServicesBuildTimeConfig devservices();
}
