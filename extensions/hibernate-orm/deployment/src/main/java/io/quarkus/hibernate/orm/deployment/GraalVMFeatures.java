package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.hibernate.orm.deployment.ClassNames.GENERATORS;
import static io.quarkus.hibernate.orm.deployment.ClassNames.OPTIMIZERS;

import java.util.stream.Stream;

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

    // TODO try to limit registration to those that are actually needed, based on configuration + mapping.
    //   https://github.com/quarkusio/quarkus/pull/32433#issuecomment-1497615958
    //   See also io.quarkus.hibernate.orm.deployment.JpaJandexScavenger.enlistClassReferences for
    //   the beginning of a solution (which only handles custom types, not references by name such as 'sequence').
    @BuildStep
    ReflectiveClassBuildItem registerGeneratorAndOptimizerClassesForReflections() {
        return ReflectiveClassBuildItem
                .builder(Stream.concat(GENERATORS.stream(), OPTIMIZERS.stream()).map(DotName::toString).toArray(String[]::new))
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

    // Workaround for https://hibernate.atlassian.net/browse/HHH-18975
    @BuildStep
    ReflectiveClassBuildItem registerNamingStrategiesForReflections() {
        return ReflectiveClassBuildItem
                .builder(ClassNames.NAMING_STRATEGIES.stream().map(DotName::toString).toArray(String[]::new))
                .reason(ClassNames.GRAAL_VM_FEATURES.toString())
                .build();
    }

}
