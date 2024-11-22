package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

public class SmallRyeConfigProcessor {

    @BuildStep
    NativeImageResourceBuildItem registerResources() {
        // Accessed by SmallRye Config to load the default configuration
        return new NativeImageResourceBuildItem("application.properties", "META-INF/microprofile-config.properties");
    }

}
