package io.quarkus.deployment;

import static io.smallrye.config.SmallRyeConfigBuilder.META_INF_MICROPROFILE_CONFIG_PROPERTIES;

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
        // Accessed by io.smallrye.config.SmallRyeConfigBuilder.getDefaultSources
        resources.produce(new NativeImageResourceBuildItem(META_INF_MICROPROFILE_CONFIG_PROPERTIES));
    }
}
