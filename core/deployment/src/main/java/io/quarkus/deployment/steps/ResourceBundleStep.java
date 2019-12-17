package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;

public class ResourceBundleStep {

    @BuildStep
    public NativeImageResourceBundleBuildItem nativeImageResourceBundle() {
        /*
         * The following resource bundle sometimes needs to be included into the native image with JDK 11.
         * This might no longer be required if GraalVM auto-includes it in a future release.
         * See https://github.com/oracle/graal/issues/2005 for more details about it.
         */
        return new NativeImageResourceBundleBuildItem("sun.security.util.Resources");
    }
}
