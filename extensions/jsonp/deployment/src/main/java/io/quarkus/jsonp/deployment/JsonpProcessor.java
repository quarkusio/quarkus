package io.quarkus.jsonp.deployment;

import org.glassfish.json.JsonProviderImpl;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;

public class JsonpProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JsonProviderImpl.class.getName()));
    }
}
