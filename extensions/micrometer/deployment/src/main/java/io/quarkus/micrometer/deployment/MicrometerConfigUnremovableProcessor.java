package io.quarkus.micrometer.deployment;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class MicrometerConfigUnremovableProcessor {

    /**
     * config objects are beans, but they are not unremovable by default
     */
    @BuildStep
    UnremovableBeanBuildItem mpConfigAsBean() {
        return UnremovableBeanBuildItem.beanTypes(MicrometerConfig.class);
    }

}
