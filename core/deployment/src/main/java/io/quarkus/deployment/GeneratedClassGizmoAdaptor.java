package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedClassGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> classOutput;
    private final boolean applicationClass;

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> classOutput, boolean applicationClass) {
        this.classOutput = classOutput;
        this.applicationClass = applicationClass;
    }

    @Override
    public void write(String s, byte[] bytes) {
        classOutput.produce(new GeneratedClassBuildItem(applicationClass, s, bytes));
    }

}
