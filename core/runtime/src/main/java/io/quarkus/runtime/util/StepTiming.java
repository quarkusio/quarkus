package io.quarkus.runtime.util;

import io.quarkus.runtime.StartupContext;

public class StepTiming {

    public static final String PRINT_STARTUP_TIMES = "quarkus.debug.print-startup-times";

    private static boolean stepTimingEnabled;
    private static long stepTimingStart;

    public static void configureEnabled() {
        stepTimingEnabled = System.getProperty(PRINT_STARTUP_TIMES, "false").equalsIgnoreCase("true");
    }

    public static void configureStart() {
        stepTimingStart = System.currentTimeMillis();
    }

    public static void printStepTime(StartupContext startupContext) {
        if (!stepTimingEnabled) {
            return;
        }
        long stepTimingStop = System.currentTimeMillis();
        String currentBuildStepName = startupContext.getCurrentBuildStepName();
        System.out
                .println("Build step " + currentBuildStepName + " completed in: " + (stepTimingStop - stepTimingStart) + "ms");
        stepTimingStart = System.currentTimeMillis();
    }
}
