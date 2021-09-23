package io.quarkus.runtime.graal;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import com.oracle.svm.core.annotate.AutomaticFeature;

/**
 * Technically, this should live in extensions/awt,
 * but currently all code that relies on JAXB
 * requires at the very least sun.java2d package to be runtime initialized.
 *
 * Having sun.java2d code initialized at build time caused issues,
 * which is why a substitution was set in place to avoid such code making it to the binary:
 * https://github.com/quarkusio/quarkus/commit/ef87e5567cf3ac462a3f12aad4b5b530d9220223
 *
 * So, as long as JAXB graphics code has not been excluded completely from JAXB,
 * it is safer to define all image related packages to be runtime initialized directly in core.
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
