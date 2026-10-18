package io.quarkus.deployment.steps;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.runtime.annotations.RegisterResources;

public class RegisterResourcesBuildStep {

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageResourcePatternsBuildItem> resources) {
        for (var annotationInstance : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterResources.class.getName()))) {
            var builder = NativeImageResourcePatternsBuildItem.builder();
            var globsValue = annotationInstance.value("globs");
            if (globsValue != null) {
                builder.includeGlobs(globsValue.asStringArray());
            }
            resources.produce(builder.build());
        }
    }
}
