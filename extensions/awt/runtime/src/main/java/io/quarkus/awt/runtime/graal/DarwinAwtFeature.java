package io.quarkus.awt.runtime.graal;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import com.oracle.svm.core.annotate.AutomaticFeature;

import io.quarkus.runtime.util.JavaVersionUtil;

@AutomaticFeature
@Platforms({ Platform.DARWIN_AMD64.class, Platform.DARWIN_AARCH64.class })
public class DarwinAwtFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        if (JavaVersionUtil.isJava17OrHigher()) {
            final RuntimeClassInitializationSupport runtimeInit = ImageSingletons
                    .lookup(RuntimeClassInitializationSupport.class);
            final String reason = "Quarkus run time init for AWT in Darwin";
            runtimeInit.initializeAtRunTime("sun.lwawt.macosx", reason);
        }
    }
}
