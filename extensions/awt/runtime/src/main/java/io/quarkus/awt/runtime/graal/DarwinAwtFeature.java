package io.quarkus.awt.runtime.graal;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

@Platforms({ Platform.DARWIN_AMD64.class, Platform.DARWIN_AARCH64.class })
public class DarwinAwtFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        // Quarkus run time init for AWT in Darwin
        RuntimeClassInitialization.initializeAtRunTime("sun.lwawt.macosx");
    }
}
