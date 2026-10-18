package io.quarkus.deployment.recording;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;

public class AnnotationProxyBuildStep {

    @BuildStep
    public AnnotationProxyBuildItem build(ApplicationIndexBuildItem applicationIndex) {
        return new AnnotationProxyBuildItem(new AnnotationProxyProvider(applicationIndex.getIndex()));
    }

}
