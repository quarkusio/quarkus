package io.quarkus.deployment;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ConstantBootstrapBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.smallrye.common.annotation.Experimental;

@Experimental("Interim adapter class, to be replaced by an injection-based mechanism")
public class GeneratedClassGizmo2Adaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> generatedClasses;
    private final BuildProducer<GeneratedResourceBuildItem> generatedResources;
    private final BuildProducer<ConstantBootstrapBuildItem> constantBootstraps;
    private final Predicate<String> applicationClassPredicate;

    /**
     * @deprecated Use constructors which accept build producers for {@link ConstantBootstrapBuildItem}.
     *
     * @param generatedClasses the generated classed build producer
     * @param generatedResources the generated resources build producer
     * @param applicationClass the application class flag
     */
    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            final BuildProducer<GeneratedResourceBuildItem> generatedResources, boolean applicationClass) {
        this(generatedClasses, generatedResources, (Predicate<String>) ignored -> applicationClass);
    }

    /**
     * @deprecated Use constructors which accept build producers for {@link ConstantBootstrapBuildItem}.
     *
     * @param generatedClasses the generated classed build producer
     * @param generatedResources the generated resources build producer
     * @param applicationClassPredicate the application class predicate
     */
    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            Predicate<String> applicationClassPredicate) {
        this(generatedClasses, generatedResources, null, applicationClassPredicate);
    }

    /**
     * @deprecated Use constructors which accept build producers for {@link ConstantBootstrapBuildItem}.
     *
     * @param generatedClasses the generated classed build producer
     * @param generatedResources the generated resources build producer
     * @param generatedToBaseNameFunction the generated-to-base-name function
     */
    @Deprecated
    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            Function<String, String> generatedToBaseNameFunction) {
        this(generatedClasses, generatedResources,
                (Predicate<String>) s -> isApplicationClass(generatedToBaseNameFunction.apply(s)));
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps,
            Predicate<String> applicationClassPredicate) {
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.constantBootstraps = constantBootstraps;
        this.applicationClassPredicate = applicationClassPredicate;
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps,
            boolean applicationClass) {
        this(generatedClasses, generatedResources, constantBootstraps, (Predicate<String>) ignored -> applicationClass);
    }

    public GeneratedClassGizmo2Adaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps,
            Function<String, String> generatedToBaseNameFunction) {
        this(generatedClasses, generatedResources, constantBootstraps,
                (Predicate<String>) s -> isApplicationClass(generatedToBaseNameFunction.apply(s)));
    }

    @Override
    public void write(String resourceName, byte[] bytes) {
        if (resourceName.endsWith(".class")) {
            String className = resourceName.substring(0, resourceName.length() - 6).replace('/', '.');
            generatedClasses.produce(
                    new GeneratedClassBuildItem(applicationClassPredicate.test(className), className, bytes));
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
