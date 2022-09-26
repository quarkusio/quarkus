package io.quarkus.hibernate.orm.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;

/**
 * Activates the native-image features included in the module
 * org.hibernate:hibernate-graalvm.
 */
@BuildSteps
public class GraalVMFeatures {

    @BuildStep
    NativeImageFeatureBuildItem staticNativeImageFeature() {
        return new NativeImageFeatureBuildItem("org.hibernate.graalvm.internal.GraalVMStaticFeature");
    }

    @BuildStep
    NativeImageFeatureBuildItem queryParsingSupportFeature() {
        return new NativeImageFeatureBuildItem("org.hibernate.graalvm.internal.QueryParsingSupport");
    }

}
