package io.quarkus.runtime;

import java.math.BigDecimal;

import org.jboss.logging.Logger;

/**
 * Class that is responsible for printing out timing results.
 * <p>
 * It is modified on substrate by {@link io.quarkus.runtime.graal.TimingReplacement}, in that mainStarted it rewritten to
 * actually update the start time.
 */
public class Timing {

    private static volatile long bootStartTime = -1;

    private static volatile long bootStopTime = -1;

    private static volatile String httpServerInfo = "";

    public static void staticInitStarted() {
        if (bootStartTime < 0) {
            bootStartTime = System.nanoTime();
        }
    }

    public static void staticInitStopped() {
        if (bootStopTime < 0) {
            bootStopTime = System.nanoTime();
        }
    }

    /**
     * An extension providing the HTTP server should set the current info (port, host, etc.) in a recorder method of a
     * RUNTIME_INIT build step. Note that it is not possible to inspect thee RUN_TIME config properties through MP Config.
     *
     * @param info
     */
    public static void setHttpServer(String info) {
        httpServerInfo = info;
    }

    /**
     * This method is replaced by substrate
     */
    public static void mainStarted() {
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
        bootStartTime = -1;
    }

    public static void printStopTime() {
        final long stopTimeNanoSeconds = System.nanoTime() - bootStopTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(stopTimeNanoSeconds);
        logger.infof("Quarkus stopped in %ss", secondsRepresentation);
        bootStopTime = -1;
    }

    public static BigDecimal convertToBigDecimalSeconds(final long timeNanoSeconds) {
        final BigDecimal secondsRepresentation = BigDecimal.valueOf(timeNanoSeconds) // As nanoseconds
                .divide(BigDecimal.valueOf(1_000_000), BigDecimal.ROUND_HALF_UP) // Convert to milliseconds, discard remaining digits while rounding
                .divide(BigDecimal.valueOf(1_000), 3, BigDecimal.ROUND_HALF_UP); // Convert to seconds, while preserving 3 digits
        return secondsRepresentation;
    }

}
