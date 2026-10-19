package io.quarkus.arc.deployment;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Predicate;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ConstantBootstrapBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.smallrye.common.annotation.Experimental;

@Experimental("Interim adapter class, to be replaced by an injection-based mechanism")
public class GeneratedBeanGizmo2Adaptor implements ClassOutput {

    private final BuildProducer<GeneratedBeanBuildItem> classOutput;
    private final Predicate<String> applicationClassPredicate;
    private final BuildProducer<ConstantBootstrapBuildItem> constantBootstraps;

    @Deprecated(since = "4.0")
    public GeneratedBeanGizmo2Adaptor(BuildProducer<GeneratedBeanBuildItem> classOutput) {
        this(classOutput, (BuildProducer<ConstantBootstrapBuildItem>) null);
    }

    @Deprecated(since = "4.0")
    public GeneratedBeanGizmo2Adaptor(BuildProducer<GeneratedBeanBuildItem> classOutput,
            Predicate<String> applicationClassPredicate) {
        this(classOutput, applicationClassPredicate, null);
    }

    public GeneratedBeanGizmo2Adaptor(BuildProducer<GeneratedBeanBuildItem> classOutput,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps) {
        this(classOutput, new Predicate<>() {

            @Override
            public boolean test(String t) {
                return true;
            }
        }, constantBootstraps);
    }

    public GeneratedBeanGizmo2Adaptor(BuildProducer<GeneratedBeanBuildItem> classOutput,
            Predicate<String> applicationClassPredicate,
            BuildProducer<ConstantBootstrapBuildItem> constantBootstraps) {
        this.classOutput = classOutput;
        this.applicationClassPredicate = applicationClassPredicate;
        this.constantBootstraps = constantBootstraps;
    }

    @Override
    public void write(String className, byte[] bytes) {
        String classNameWithoutSuffix = className.substring(0, className.length() - 6);
        classOutput.produce(new GeneratedBeanBuildItem(classNameWithoutSuffix, bytes, null,
                applicationClassPredicate.test(classNameWithoutSuffix)));
    }

    @Override
    public void registerBootstrapMethod(ClassDesc owner, String name, MethodTypeDesc type) {
        if (constantBootstraps != null) {
            constantBootstraps.produce(new ConstantBootstrapBuildItem(null, owner, name, type));
        } else {
            throw new IllegalStateException("To use dynamic constants or indy from generated code at this site, "
                    + "you must provide a build producer for ConstantBootstrapBuildItem to the constructor "
                    + "of GeneratedBeanGizmo2Adaptor");
        }
    }
}
