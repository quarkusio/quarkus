package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class StorkBinderProcessor {

    static final String OBSERVABLE_CLIENT = "io.smallrye.stork.api.Service";
    static final String METRICS_BEAN_CLASS = "io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean";

    static final Class<?> OBSERVABLE_CLIENT_CLASS = MicrometerRecorder.getClassForName(OBSERVABLE_CLIENT);

    static class StorkMetricsSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return OBSERVABLE_CLIENT_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder().stork());
        }
    }

    @BuildStep(onlyIf = StorkMetricsSupportEnabled.class)
    AdditionalBeanBuildItem addStorkObservationCollector() {
        return AdditionalBeanBuildItem.unremovableOf(METRICS_BEAN_CLASS);
    }

}
