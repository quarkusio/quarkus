package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;

public class QuarkusProcessor {

    @BuildStep
    ReflectiveMethodBuildItem registerReflectiveMethods() {
        // Called reflectively when generating quarkus.uuid
        return new ReflectiveMethodBuildItem("sun.security.provider.NativePRNG", "<init>",
                java.security.SecureRandomParameters.class);
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resources) {
        // Accessed by io.quarkus.runtime.configuration.ApplicationPropertiesConfigSourceLoader.InClassPath.getConfigSources
        resources.produce(new NativeImageResourceBuildItem("application.properties"));
        // Accessed by io.smallrye.config.SmallRyeConfigBuilder.getPropertiesSources
        resources.produce(new NativeImageResourceBuildItem("META-INF/microprofile-config.properties"));
    }
}
