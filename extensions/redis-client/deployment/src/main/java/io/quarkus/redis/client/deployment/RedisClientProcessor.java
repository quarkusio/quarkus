package io.quarkus.redis.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.vertx.redis.client.impl.types.BulkType;

public class RedisClientProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.REDIS_CLIENT);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.REDIS_CLIENT.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem registerAPIsProducer() {
        return AdditionalBeanBuildItem.unremovableOf("io.quarkus.redis.client.runtime.RedisAPIProducer");
    }

    @BuildStep
    HealthBuildItem addHealthCheck(RedisBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.redis.client.runtime.health.RedisHealthCheck", buildTimeConfig.healthEnabled);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem initializeBulkTypeDuringRuntime() {
        return new RuntimeInitializedClassBuildItem(BulkType.class.getName());
    }
}
