package io.quarkus.micrometer.deployment;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

// Executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
public class MicrometerConfigAlwaysEnabledProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MICROMETER);
    }

    /**
     * config objects are beans, but they are not unremovable by default
     */
    @BuildStep
    UnremovableBeanBuildItem mpConfigAsBean() {
        return UnremovableBeanBuildItem.beanTypes(MicrometerConfig.class);
    }

}
