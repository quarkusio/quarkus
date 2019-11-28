package io.quarkus.runtime;

import java.math.BigDecimal;
import java.util.logging.Handler;

import org.jboss.logging.Logger;

import io.quarkus.runtime.logging.InitialConfigurator;

/**
 * Class that is responsible for printing out timing results.
 * <p>
 * It is modified in native mode by {@link io.quarkus.runtime.graal.TimingReplacement}, in that mainStarted it rewritten to
 * actually update the start time.
 */
public class Timing {

    private static volatile long bootStartTime = -1;

    private static volatile long bootStopTime = -1;

    private static volatile String httpServerInfo = "";

    private static final String UNSET_VALUE = "<<unset>>";

    public static void staticInitStarted() {
        if (bootStartTime < 0) {
            bootStartTime = System.nanoTime();
        }
    }

    public static void staticInitStarted(ClassLoader cl) {
        try {
            Class<?> realTiming = cl.loadClass(Timing.class.getName());
            realTiming.getMethod("staticInitStarted").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
     * This method is replaced in native mode
     */
    public static void mainStarted() {
    }

    public static void restart() {
        bootStartTime = System.nanoTime();
    }

    public static void restart(ClassLoader cl) {
        try {
            Class<?> realTiming = cl.loadClass(Timing.class.getName());
            realTiming.getMethod("restart").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void printStartupTime(String name, String version, String quarkusVersion, String features, String profile,
            boolean liveCoding) {
        final long bootTimeNanoSeconds = System.nanoTime() - bootStartTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        //Use a BigDecimal so we can render in seconds with 3 digits precision, as requested:
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(bootTimeNanoSeconds);
        String safeAppName = (name == null || name.trim().isEmpty()) ? UNSET_VALUE : name;
        String safeAppVersion = (version == null || version.trim().isEmpty()) ? UNSET_VALUE : version;
        if (UNSET_VALUE.equals(safeAppName) || UNSET_VALUE.equals(safeAppVersion)) {
            logger.infof("Quarkus %s started in %ss. %s", quarkusVersion, secondsRepresentation, httpServerInfo);
        } else {
            logger.infof("%s %s (running on Quarkus %s) started in %ss. %s", name, version, quarkusVersion,
                    secondsRepresentation, httpServerInfo);
        }
        logger.infof("Profile %s activated. %s", profile, liveCoding ? "Live Coding activated." : "");
        logger.infof("Installed features: [%s]", features);
        bootStartTime = -1;
    }

    public static void printStopTime(String name) {
        final long stopTimeNanoSeconds = System.nanoTime() - bootStopTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(stopTimeNanoSeconds);
        logger.infof("%s stopped in %ss",
                (UNSET_VALUE.equals(name) || name == null || name.trim().isEmpty()) ? "Quarkus" : name,
                secondsRepresentation);
        bootStopTime = -1;

        /**
         * We can safely close log handlers after stop time has been printed.
         */
        Handler[] handlers = InitialConfigurator.DELAYED_HANDLER.clearHandlers();
        for (Handler handler : handlers) {
            try {
                handler.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public static BigDecimal convertToBigDecimalSeconds(final long timeNanoSeconds) {
        final BigDecimal secondsRepresentation = BigDecimal.valueOf(timeNanoSeconds) // As nanoseconds
                .divide(BigDecimal.valueOf(1_000_000), BigDecimal.ROUND_HALF_UP) // Convert to milliseconds, discard remaining digits while rounding
                .divide(BigDecimal.valueOf(1_000), 3, BigDecimal.ROUND_HALF_UP); // Convert to seconds, while preserving 3 digits
        return secondsRepresentation;
    }

}
