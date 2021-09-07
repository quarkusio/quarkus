package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.ConnectorProviderBuildItem;

public class SmallRyeReactiveMessagingMqttProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_MQTT);
    }

    @BuildStep
    ConnectorProviderBuildItem connectorProviderBuildItem() {
        return new ConnectorProviderBuildItem("smallrye-mqtt");
    }
}
