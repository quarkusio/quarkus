package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class ReactiveMessagingProcessor {

    static final String MESSAGE_OBSERVATION_COLLECTOR = "io.smallrye.reactive.messaging.observation.MessageObservationCollector";
    static final String METRICS_BEAN_CLASS = "io.quarkus.micrometer.runtime.binder.reactivemessaging.MicrometerObservationCollector";
    static final Class<?> MESSAGE_OBSERVATION_COLLECTOR_CLASS = MicrometerRecorder
            .getClassForName(MESSAGE_OBSERVATION_COLLECTOR);

    static class ReactiveMessagingSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return MESSAGE_OBSERVATION_COLLECTOR_CLASS != null &&
                    mConfig.checkBinderEnabledWithDefault(mConfig.binder().messaging());
        }
    }

    @BuildStep(onlyIf = ReactiveMessagingSupportEnabled.class)
    AdditionalBeanBuildItem createCDIEventConsumer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(METRICS_BEAN_CLASS)
                .setUnremovable()
                .build();
    }
}
