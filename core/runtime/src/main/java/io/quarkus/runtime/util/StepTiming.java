package io.quarkus.runtime.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
        System.out.printf("%1$tF %1$tT,%1$tL Build step %2$s completed in: %3$sms%n",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(stepTimingStop), ZoneId.systemDefault()),
                currentBuildStepName,
                stepTimingStop - stepTimingStart);
        stepTimingStart = System.currentTimeMillis();
    }
}
