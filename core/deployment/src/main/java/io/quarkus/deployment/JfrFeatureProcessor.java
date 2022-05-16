package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;

public class JfrFeatureProcessor {

    /**
     * Work around for https://github.com/quarkusio/quarkus/issues/25501 until
     * https://github.com/oracle/graal/issues/4543 gets resolved
     *
     * @return
     */
    @BuildStep
    NativeImageConfigBuildItem nativeImageConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addRuntimeReinitializedClass("com.sun.management.internal.PlatformMBeanProviderImpl");
        return builder.build();
    }

}
