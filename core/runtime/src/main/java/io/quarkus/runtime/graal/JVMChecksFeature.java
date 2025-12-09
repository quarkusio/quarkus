package io.quarkus.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;

import io.quarkus.runtime.JVMUnsafeWarningsControl;

/**
 * This is a horrible hack to disable the Unsafe-related warnings that are printed on startup:
 * we know about the problem, we're working on it, and there's no need to print a warning scaring our
 * users with it.
 */
public class JVMChecksFeature implements Feature {

    @Override
    public void duringSetup(Feature.DuringSetupAccess access) {
        JVMUnsafeWarningsControl.disableUnsafeRelatedWarnings();
    }

}
