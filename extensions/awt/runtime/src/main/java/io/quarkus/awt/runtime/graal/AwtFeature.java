package io.quarkus.awt.runtime.graal;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import com.oracle.svm.core.annotate.AutomaticFeature;

/**
 * Note that this initialization s not enough if user wants to deserialize actual images
 * (e.g. from XML). AWT Extension must be loaded for decoding JDK supported image formats.
 */
@AutomaticFeature
public class AwtFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        final RuntimeClassInitializationSupport runtimeInit = ImageSingletons.lookup(RuntimeClassInitializationSupport.class);
        final String reason = "Quarkus run time init for AWT";
        runtimeInit.initializeAtRunTime("com.sun.imageio", reason);
        runtimeInit.initializeAtRunTime("java.awt", reason);
        runtimeInit.initializeAtRunTime("javax.imageio", reason);
        runtimeInit.initializeAtRunTime("sun.awt", reason);
        runtimeInit.initializeAtRunTime("sun.font", reason);
        runtimeInit.initializeAtRunTime("sun.java2d", reason);
    }
}
