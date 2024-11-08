package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.hibernate.orm.deployment.ClassNames.GENERATORS;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Activates the native-image features included in the module
 * org.hibernate:hibernate-graalvm.
 */
@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class GraalVMFeatures {

    @BuildStep
    NativeImageFeatureBuildItem staticNativeImageFeature() {
        return new NativeImageFeatureBuildItem("org.hibernate.graalvm.internal.GraalVMStaticFeature");
    }

    // Workaround for https://hibernate.atlassian.net/browse/HHH-16439
    @BuildStep
    ReflectiveClassBuildItem registerGeneratorClassesForReflections() {
        return ReflectiveClassBuildItem.builder(GENERATORS.stream().map(DotName::toString).toArray(String[]::new))
                .reason(ClassNames.GRAAL_VM_FEATURES.toString())
                .build();
    }

    // Workaround for https://hibernate.atlassian.net/browse/HHH-16809
    // See https://github.com/hibernate/hibernate-orm/pull/6815#issuecomment-1662197545
    @BuildStep
    ReflectiveClassBuildItem registerJdbcArrayTypesForReflection() {
        return ReflectiveClassBuildItem
                .builder(ClassNames.JDBC_JAVA_TYPES.stream().map(d -> d.toString() + "[]").toArray(String[]::new))
                .reason(ClassNames.GRAAL_VM_FEATURES.toString())
                .build();
    }

    // Workaround for https://hibernate.atlassian.net/browse/HHH-18875
    // See https://hibernate.zulipchat.com/#narrow/channel/132094-hibernate-orm-dev/topic/StandardStack.20and.20reflection
    @BuildStep
    ReflectiveClassBuildItem registerStandardStackElementTypesForReflection() {
        return ReflectiveClassBuildItem
                .builder(ClassNames.STANDARD_STACK_ELEMENT_TYPES.stream().map(d -> d.toString() + "[]").toArray(String[]::new))
                .reason("Workaround for https://hibernate.atlassian.net/browse/HHH-18875")
                .build();
    }

}
