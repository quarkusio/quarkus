package io.quarkus.arc.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedBeanGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedBeanBuildItem> classOutput;

    public GeneratedBeanGizmoAdaptor(BuildProducer<GeneratedBeanBuildItem> classOutput) {
        this.classOutput = classOutput;
    }

    @Override
    public void write(String s, byte[] bytes) {
        classOutput.produce(new GeneratedBeanBuildItem(s, bytes));
    }

}
