package io.quarkus.runtime.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.StartupContext;

public class StepTiming {

    public static final String PRINT_STARTUP_TIMES = "quarkus.debug.print-startup-times";

    private static boolean stepTimingEnabled;
    private static long stepTimingStart;

    public static void configureEnabled() {
        stepTimingEnabled = System.getProperty(PRINT_STARTUP_TIMES, "false").equalsIgnoreCase("true");
    }

    public static void configureStart() {
        stepTimingStart = System.nanoTime();
    }

    public static void printStepTime(StartupContext startupContext) {
        if (!stepTimingEnabled) {
            return;
        }
        String currentBuildStepName = startupContext.getCurrentBuildStepName();
        System.out.printf("%1$tF %1$tT,%1$tL Build step %2$s completed in: %3$sms%n",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()),
                currentBuildStepName,
                duration(System.nanoTime(), stepTimingStart));
        stepTimingStart = System.nanoTime();
    }

    private static long duration(long ended, long started) {
        return TimeUnit.MILLISECONDS.convert(ended - started, TimeUnit.NANOSECONDS);
    }
}
