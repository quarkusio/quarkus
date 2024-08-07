package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class StorkBinderProcessor {

    static final String OBSERVABLE_CLIENT = "io.smallrye.stork.api.Service";
    static final String METRICS_BEAN_CLASS = "io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean";

    static class StorkMetricsSupportEnabled implements BooleanSupplier {
        private final MicrometerConfig mConfig;

        StorkMetricsSupportEnabled(MicrometerConfig mConfig) {
            this.mConfig = mConfig;
        }

        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(OBSERVABLE_CLIENT)
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder.stork);
        }
    }

    @BuildStep(onlyIf = StorkMetricsSupportEnabled.class)
    AdditionalBeanBuildItem addStorkObservationCollector() {
        return AdditionalBeanBuildItem.unremovableOf(METRICS_BEAN_CLASS);
    }

}
