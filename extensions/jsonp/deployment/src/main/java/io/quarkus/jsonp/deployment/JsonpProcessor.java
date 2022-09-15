package io.quarkus.jsonp.deployment;

import org.eclipse.parsson.JsonProviderImpl;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class JsonpProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JsonProviderImpl.class.getName()));
    }
}
