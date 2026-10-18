package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.reactivemessaging.amqp.runtime.AmqpClientConfigCustomizer;

public class SmallRyeReactiveMessagingAmqpProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MESSAGING_AMQP);
    }

    @BuildStep
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(AmqpClientConfigCustomizer.class)
                .setUnremovable()
                .build();
    }
}
