package io.quarkus.awt.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

/**
 * Note that this initialization is not enough if user wants to deserialize actual images
 * (e.g. from XML). AWT Extension must be loaded for decoding JDK supported image formats.
 */
public class AwtFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        // Quarkus run time init for AWT
        RuntimeClassInitialization.initializeAtRunTime(
                "com.sun.imageio",
                "java.awt",
                "javax.imageio",
                "sun.awt",
                "sun.font",
                "sun.java2d");
    }
}
