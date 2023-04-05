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
                .build();
    }

}
