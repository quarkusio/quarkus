package io.quarkus.runtime;

/**
 * Stores the optimizations to execute for SnapStart.
 */
public class SnapStartRecorder {

    /**
     * Whether SnapStart is enabled.
     */
    public static boolean enabled = false;

    /**
     * Whether to perform a full warmup.
     */
    public static boolean fullWarmup = false;
}
