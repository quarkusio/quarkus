package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class RedisBinderProcessor {

    static final String OBSERVABLE_CLIENT = "io.quarkus.redis.runtime.client.ObservableRedis";
    static final String METRICS_BEAN_CLASS = "io.quarkus.micrometer.runtime.binder.redis.RedisMetricsBean";

    static final Class<?> OBSERVABLE_CLIENT_CLASS = MicrometerRecorder.getClassForName(OBSERVABLE_CLIENT);

    static class RedisMetricsSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return OBSERVABLE_CLIENT_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder().redis());
        }
    }

    @BuildStep(onlyIf = RedisMetricsSupportEnabled.class)
    AdditionalBeanBuildItem addRedisClientMetric() {
        return AdditionalBeanBuildItem.unremovableOf(METRICS_BEAN_CLASS);
    }

}
