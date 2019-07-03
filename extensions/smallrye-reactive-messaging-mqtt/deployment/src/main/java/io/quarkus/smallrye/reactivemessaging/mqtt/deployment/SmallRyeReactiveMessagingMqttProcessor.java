package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SmallRyeReactiveMessagingMqttProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING_MQTT);
    }
}