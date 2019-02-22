package org.jboss.shamrock.deployment.recording;

import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AnnotationProxyBuildItem;
import org.jboss.shamrock.deployment.builditem.ApplicationIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;

public class AnnotationProxyBuildStep {

    @BuildStep
    public AnnotationProxyBuildItem build(BuildProducer<GeneratedClassBuildItem> generatedClass, ApplicationIndexBuildItem applicationIndex) {
        return new AnnotationProxyBuildItem(new AnnotationProxyProvider(new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(false, name, data));
            }
        }, applicationIndex.getIndex()));
    }

}
