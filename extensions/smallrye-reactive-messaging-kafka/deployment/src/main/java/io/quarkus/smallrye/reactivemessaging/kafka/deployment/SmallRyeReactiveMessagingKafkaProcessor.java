package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SmallRyeReactiveMessagingKafkaProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_KAFKA);
    }
}
