package io.quarkus.runtime;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Stores the optimizations to execute for SnapStart.
 */
@Recorder
public class SnapStartRecorder {

    public static boolean enabled = false;
    public static boolean fullWarmup = false;

    public void register(boolean fw) {
        enabled = true;
        fullWarmup = fw;
    }
}
