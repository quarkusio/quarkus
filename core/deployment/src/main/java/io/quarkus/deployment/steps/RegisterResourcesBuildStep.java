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
            var includeGlobsValue = annotationInstance.value("includeGlobs");
            if (includeGlobsValue != null) {
                builder.includeGlobs(includeGlobsValue.asStringArray());
            }
            var includePatternsValue = annotationInstance.value("includePatterns");
            if (includePatternsValue != null) {
                builder.includePatterns(includePatternsValue.asStringArray());
            }
            var excludeGlobsValue = annotationInstance.value("excludeGlobs");
            if (excludeGlobsValue != null) {
                builder.excludeGlobs(excludeGlobsValue.asStringArray());
            }
            var excludePatternsValue = annotationInstance.value("excludePatterns");
            if (excludePatternsValue != null) {
                builder.excludePatterns(excludePatternsValue.asStringArray());
            }
            resources.produce(builder.build());
        }
    }
}