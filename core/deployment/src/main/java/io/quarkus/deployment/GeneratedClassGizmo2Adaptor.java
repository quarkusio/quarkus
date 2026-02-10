package io.quarkus.deployment;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ConstantBootstrapBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.smallrye.common.annotation.Experimental;

@Experimental("Interim adapter class, to be replaced by an injection-based mechanism")
public class GeneratedClassGizmo2Adaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> generatedClasses;
    private final BuildProducer<GeneratedResourceBuildItem> generatedResources;
    private final BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders;
    private final BuildProducer<ConstantBootstrapBuildItem> constantBootstraps;
    private final Predicate<String> applicationClassPredicate;

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps,
            boolean applicationClass) {
        this(generatedClasses, generatedResources, generatedServiceProviders, constantBootstraps,
                (String ignored) -> applicationClass);
    }

    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            boolean applicationClass) {
        this(generatedClasses, generatedResources, null, applicationClass);
    }

    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            boolean applicationClass) {
        this(generatedClasses, generatedResources, generatedServiceProviders, null, applicationClass);
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps,
            Predicate<String> applicationClassPredicate) {
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.generatedServiceProviders = generatedServiceProviders;
        this.constantBootstraps = constantBootstraps;
        this.applicationClassPredicate = applicationClassPredicate;
    }

    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            final BuildProducer<GeneratedResourceBuildItem> generatedResources,
            Predicate<String> applicationClassPredicate) {
        this(generatedClasses, generatedResources, null, applicationClassPredicate);
    }

    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            Predicate<String> applicationClassPredicate) {
        this(generatedClasses, generatedResources, generatedServiceProviders, null, applicationClassPredicate);
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps,
            Function<String, String> generatedToBaseNameFunction) {
        this(generatedClasses, generatedResources, generatedServiceProviders, constantBootstraps,
                (String s) -> isApplicationClass(generatedToBaseNameFunction.apply(s)));
    }

    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            Function<String, String> generatedToBaseNameFunction) {
        this(generatedClasses, generatedResources, null, generatedToBaseNameFunction);
    }

    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders,
            Function<String, String> generatedToBaseNameFunction) {
        this(generatedClasses, generatedResources, generatedServiceProviders, null, generatedToBaseNameFunction);
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

    //@Override
    public void registerBootstrapMethod(ClassDesc owner, String name, MethodTypeDesc type) {
        if (constantBootstraps != null) {
            constantBootstraps.produce(new ConstantBootstrapBuildItem(null, owner, name, type));
        } else {
            throw new IllegalStateException("To use dynamic constants or indy from generated code at this site, "
                    + "you must provide a build producer for ConstantBootstrapBuildItem to the constructor "
                    + "of GeneratedClassGizmo2Adaptor");
        }
    }

    public static boolean isApplicationClass(String className) {
        return QuarkusClassLoader.isApplicationClass(className);
    }

}
