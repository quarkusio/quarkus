package io.quarkus.runtime;

import java.math.BigDecimal;

import org.jboss.logging.Logger;

/**
 * Class that is responsible for printing out timing results.
 */
public class Timing {

    private static volatile long bootStartTime;

    private static volatile long bootStopTime;

    private static volatile String httpServerInfo = "";

    /**
     * Mark the point when the server has begun starting.
     */
    public static void markStart() {
        marked = true;
        bootStartTime = System.nanoTime();
    }

    /**
     * Mark the point when the server has begun stopping.
     */
    public static void markStop() {
        bootStopTime = System.nanoTime();
    }

    /**
     * An extension providing the HTTP server should set the current info (port, host, etc.) in a template method of a
     * RUNTIME_INIT build step. Note that it is not possible to inspect thee RUN_TIME config properties through MP Config.
     * 
     * @param info
     */
    public static void setHttpServer(String info) {
        httpServerInfo = info;
    }

    private static volatile boolean marked;

    /**
     * Mark the point when the server has begun the main method for the first time.
     */
    public static void markFirstStart() {
        // For dev mode and unit tests, subsequent restarts will be recorded from the main method.
        if (marked) {
            marked = false;
        } else {
            markStart();
        }
    }

    /**
     * Mark the point when the server has begun the main method for the first time. Prevents the next
     * main method start from recording a time.
     */
    public static void markStaticInitStart() {
        marked = true;
        markStart();
    }

    public static void restart() {
        bootStartTime = System.nanoTime();
    }

    public static void printStartupTime(String version, String features) {
        final long bootTimeNanoSeconds = System.nanoTime() - bootStartTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        //Use a BigDecimal so we can render in seconds with 3 digits precision, as requested:
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(bootTimeNanoSeconds);
        logger.infof("Quarkus %s started in %ss. %s", version, secondsRepresentation, httpServerInfo);
        logger.infof("Installed features: [%s]", features);
    }

    public static void printStopTime() {
        final long stopTimeNanoSeconds = System.nanoTime() - bootStopTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(stopTimeNanoSeconds);
        logger.infof("Quarkus stopped in %ss", secondsRepresentation);
    }

    public static BigDecimal convertToBigDecimalSeconds(final long timeNanoSeconds) {
        final BigDecimal secondsRepresentation = BigDecimal.valueOf(timeNanoSeconds) // As nanoseconds
                .divide(BigDecimal.valueOf(1_000_000), BigDecimal.ROUND_HALF_UP) // Convert to milliseconds, discard remaining digits while rounding
                .divide(BigDecimal.valueOf(1_000), 3, BigDecimal.ROUND_HALF_UP); // Convert to seconds, while preserving 3 digits
        return secondsRepresentation;
    }

}
