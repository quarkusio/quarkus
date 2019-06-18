package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SmallRyeReactiveMessagingAmqpProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING_AMQP);
    }

}
