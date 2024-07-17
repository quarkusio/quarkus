package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.reactivemessaging.mqtt.runtime.MqttClientConfigCustomizer;

public class SmallRyeReactiveMessagingMqttProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MESSAGING_MQTT);
    }

    @BuildStep
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(MqttClientConfigCustomizer.class)
                .setUnremovable()
                .build();
    }
}
