package io.quarkus.camel.component.infinispan.deployment;

import org.apache.camel.component.infinispan.InfinispanConfiguration;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

class CamelInfinispanProcessor {

    @BuildStep
    ReflectiveClassBuildItem reflection() {
        return new ReflectiveClassBuildItem(true, true, InfinispanConfiguration.class);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CAMEL_INFINISPAN);
    }
}
