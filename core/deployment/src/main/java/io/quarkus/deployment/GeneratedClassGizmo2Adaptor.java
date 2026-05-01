package io.quarkus.deployment;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.smallrye.common.annotation.Experimental;

@Experimental("Interim adapter class, to be replaced by an injection-based mechanism")
public class GeneratedClassGizmo2Adaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> generatedClasses;
    private final BuildProducer<GeneratedResourceBuildItem> generatedResources;
    private final BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders;
    private final Predicate<String> applicationClassPredicate;

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            final BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            boolean applicationClass) {
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.generatedServiceProviders = generatedServiceProviders;
        this.applicationClassPredicate = ignored -> applicationClass;
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            Predicate<String> applicationClassPredicate) {
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.generatedServiceProviders = generatedServiceProviders;
        this.applicationClassPredicate = applicationClassPredicate;
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            Function<String, String> generatedToBaseNameFunction) {
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.generatedServiceProviders = generatedServiceProviders;
        this.applicationClassPredicate = new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return isApplicationClass(generatedToBaseNameFunction.apply(s));
            }
        };
    }

    @Override
    public void write(String resourceName, byte[] bytes) {
        if (resourceName.endsWith(".class")) {
            String className = resourceName.substring(0, resourceName.length() - 6).replace('/', '.');
            generatedClasses.produce(
                    new GeneratedClassBuildItem(applicationClassPredicate.test(className), className, bytes));
        } else if (resourceName.startsWith("META-INF/services/")) {
            if (generatedServiceProviders == null) {
                throw new IllegalStateException(
                        "Cannot write service provider file '" + resourceName
                                + "': this GeneratedClassGizmo2Adaptor was created without a GeneratedServiceProviderBuildItem producer");
            }
            String serviceInterfaceName = resourceName.substring("META-INF/services/".length());
            for (String line : new String(bytes, StandardCharsets.UTF_8).split("\\r?\\n")) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    generatedServiceProviders.produce(new GeneratedServiceProviderBuildItem(serviceInterfaceName, line));
                }
            }
        } else {
            generatedResources.produce(
                    new GeneratedResourceBuildItem(resourceName, bytes));
        }
    }

    public static boolean isApplicationClass(String className) {
        return QuarkusClassLoader.isApplicationClass(className);
    }

}
