package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class RedisBinderProcessor {

    static final String OBSERVABLE_CLIENT = "io.quarkus.redis.runtime.client.ObservableRedis";
    static final String METRICS_BEAN_CLASS = "io.quarkus.micrometer.runtime.binder.redis.RedisMetricsBean";

    static class RedisMetricsSupportEnabled implements BooleanSupplier {
        private final MicrometerConfig mConfig;

        RedisMetricsSupportEnabled(MicrometerConfig mConfig) {
            this.mConfig = mConfig;
        }

        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(OBSERVABLE_CLIENT)
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder.redis);
        }
    }

    @BuildStep(onlyIf = RedisMetricsSupportEnabled.class)
    AdditionalBeanBuildItem addRedisClientMetric() {
        return AdditionalBeanBuildItem.unremovableOf(METRICS_BEAN_CLASS);
    }

}
