package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * Add support for Kafka Producer and Consumer instrumentation. Note that
 * various bits of support may not be present at deploy time. Avoid referencing
 * classes that in turn import optional dependencies.
 */
public class KafkaBinderProcessor {
    static final String KAFKA_CONSUMER_CLASS_NAME = "org.apache.kafka.clients.consumer.Consumer";
    static final Class<?> KAFKA_CONSUMER_CLASS_CLASS = MicrometerRecorder.getClassForName(KAFKA_CONSUMER_CLASS_NAME);

    static final String KAFKA_EVENT_CONSUMER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.kafka.KafkaEventObserver";

    static class KafkaSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return KAFKA_CONSUMER_CLASS_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder.kafka);
        }
    }

    @BuildStep(onlyIf = KafkaSupportEnabled.class)
    AdditionalBeanBuildItem createCDIEventConsumer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(KAFKA_EVENT_CONSUMER_CLASS_NAME)
                .setUnremovable().build();
    }
}
