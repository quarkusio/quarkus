package io.quarkus.deployment;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.smallrye.common.annotation.Experimental;

@Experimental("Interim adapter class, to be replaced by an injection-based mechanism")
public class GeneratedClassGizmo2Adaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> generatedClasses;
    private final BuildProducer<GeneratedResourceBuildItem> generatedResources;
    private final boolean applicationClass;

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            final BuildProducer<GeneratedResourceBuildItem> generatedResources, boolean applicationClass) {
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.applicationClass = applicationClass;
    }

    @Override
    public void write(String resourceName, byte[] bytes) {
        if (resourceName.endsWith(".class")) {
            String className = resourceName.substring(0, resourceName.length() - 6).replace('/', '.');
            generatedClasses.produce(
                    new GeneratedClassBuildItem(applicationClass, className, bytes));
        } else {
            generatedResources.produce(
                    new GeneratedResourceBuildItem(resourceName, bytes));
        }
    }

    public static boolean isApplicationClass(String className) {
        return QuarkusClassLoader.isApplicationClass(className);
    }

}
