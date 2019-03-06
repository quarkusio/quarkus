package io.quarkus.deployment.recording;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;

public class AnnotationProxyBuildStep {

    @BuildStep
    public AnnotationProxyBuildItem build(BuildProducer<GeneratedClassBuildItem> generatedClass,
            ApplicationIndexBuildItem applicationIndex) {
        return new AnnotationProxyBuildItem(new AnnotationProxyProvider(new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(false, name, data));
            }
        }, applicationIndex.getIndex()));
    }

}
