package io.quarkus.resteasy.reactive.spi;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedJaxRsResourceGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedJaxRsResourceBuildItem> classOutput;

    public GeneratedJaxRsResourceGizmoAdaptor(BuildProducer<GeneratedJaxRsResourceBuildItem> classOutput) {
        this.classOutput = classOutput;
    }

    @Override
    public void write(String className, byte[] bytes) {
        classOutput.produce(new GeneratedJaxRsResourceBuildItem(className, bytes));
    }

}
