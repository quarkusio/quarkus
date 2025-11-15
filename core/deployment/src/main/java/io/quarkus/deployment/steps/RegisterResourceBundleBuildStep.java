package io.quarkus.deployment.steps;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.runtime.annotations.RegisterResourceBundle;

public class RegisterResourceBundleBuildStep {

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        for (var annotationInstance : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterResourceBundle.class.getName()))) {
            var bundleNameValue = annotationInstance.value("bundleName");
            var moduleNameValue = annotationInstance.value("moduleName");
            if (moduleNameValue == null || moduleNameValue.asString().isEmpty()) {
                resourceBundle.produce(new NativeImageResourceBundleBuildItem(bundleNameValue.asString()));
            } else {
                resourceBundle.produce(new NativeImageResourceBundleBuildItem(bundleNameValue.asString(),
                        moduleNameValue.asString()));
            }
        }
    }
}
