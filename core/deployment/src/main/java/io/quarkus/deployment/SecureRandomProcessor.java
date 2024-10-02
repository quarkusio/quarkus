package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;

public class SecureRandomProcessor {

    @BuildStep
    void registerReflectiveMethods(BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {
        // Called reflectively through java.security.SecureRandom.SecureRandom()
        reflectiveMethods.produce(new ReflectiveMethodBuildItem(
                getClass().getName(),
                "sun.security.provider.NativePRNG", "<init>",
                java.security.SecureRandomParameters.class));
    }

}
