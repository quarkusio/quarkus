package io.quarkus.runtime;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Registers a CRAC resource. Must be called in static initialization phase!
 */
@Recorder
public class CracRecorder {

    public static boolean enabled = false;
    public static boolean fullWarmup = false;

    public void register(boolean fw) {
        enabled = true;
        fullWarmup = fw;
    }
}
