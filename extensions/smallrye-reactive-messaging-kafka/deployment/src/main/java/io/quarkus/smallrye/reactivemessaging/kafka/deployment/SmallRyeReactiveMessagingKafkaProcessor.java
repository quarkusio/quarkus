package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.vertx.kafka.client.consumer.impl.KafkaReadStreamImpl;

public class SmallRyeReactiveMessagingKafkaProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_KAFKA);
    }

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // Required for the throttled commit strategy
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(KafkaReadStreamImpl.class)
                        .fields(true)
                        .methods(true)
                        .constructors(true)
                        .finalFieldsWritable(true)
                        .build());
    }

}
