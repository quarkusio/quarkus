package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SmallRyeReactiveMessagingRabbitMQProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_RABBITMQ);
    }

}
