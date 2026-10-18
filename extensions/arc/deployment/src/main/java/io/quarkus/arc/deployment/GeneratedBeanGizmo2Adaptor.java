package io.quarkus.arc.deployment;

import java.util.function.Predicate;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo2.ClassOutput;
import io.smallrye.common.annotation.Experimental;

@Experimental("Interim adapter class, to be replaced by an injection-based mechanism")
public class GeneratedBeanGizmo2Adaptor implements ClassOutput {

    private final BuildProducer<GeneratedBeanBuildItem> classOutput;
    private final Predicate<String> applicationClassPredicate;

    public GeneratedBeanGizmo2Adaptor(BuildProducer<GeneratedBeanBuildItem> classOutput) {
        this(classOutput, new Predicate<>() {

            @Override
            public boolean test(String t) {
                return true;
            }
        });
    }

    public GeneratedBeanGizmo2Adaptor(BuildProducer<GeneratedBeanBuildItem> classOutput,
            Predicate<String> applicationClassPredicate) {
        this.classOutput = classOutput;
        this.applicationClassPredicate = applicationClassPredicate;
    }

    @Override
    public void write(String className, byte[] bytes) {
        String classNameWithoutSuffix = className.substring(0, className.length() - 6);
        classOutput.produce(new GeneratedBeanBuildItem(classNameWithoutSuffix, bytes, null,
                applicationClassPredicate.test(classNameWithoutSuffix)));
    }

}
