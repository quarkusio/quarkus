package io.quarkus.bootstrap.runner;

import io.quarkus.bootstrap.graal.ImageInfo;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Handler;
import org.jboss.logging.Logger;

/**
 * Class that is responsible for printing out timing results.
 * <p>
 * It is modified in native mode by {@link io.quarkus.runtime.graal.TimingReplacement}, in that mainStarted it rewritten to
 * actually update the start time.
 */
public class Timing {

    public volatile long bootStartTime = -1;

    private volatile long bootStopTime = -1;

    private volatile String httpServerInfo = "";

    private static final String UNSET_VALUE = "<<unset>>";

    private static final Timing main = new Timing();
    private static final Timing auxiliary = new Timing();

    private static Timing get(boolean anc) {
        if (anc) {
            return auxiliary;
        }
        return main;
    }

    public static void staticInitStarted(boolean auxiliary) {
        Timing t = get(auxiliary);
        if (t.bootStartTime < 0) {
            t.bootStartTime = System.nanoTime();
        }
    }

    public static void staticInitStarted(ClassLoader cl, boolean auxiliary) {
        try {
            Class<?> realTiming = cl.loadClass(Timing.class.getName());
            realTiming.getMethod("staticInitStarted", boolean.class).invoke(null, auxiliary);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void staticInitStopped(boolean auxiliary) {
        Timing t = get(auxiliary);
        if (t.bootStopTime < 0) {
            t.bootStopTime = System.nanoTime();
        }
    }

    /**
     * An extension providing the HTTP server should set the current info (port, host, etc.) in a recorder method of a
     * RUNTIME_INIT build step. Note that it is not possible to inspect thee RUN_TIME config properties through MP Config.
     *
     * @param info
     */
    public static void setHttpServer(String info, boolean auxiliary) {
        Timing t = get(auxiliary);
        t.httpServerInfo = info;
    }

    /**
     * This method is replaced in native mode
     */
    public static void mainStarted() {
    }

    public static void restart() {
        main.bootStartTime = System.nanoTime();
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
            boolean liveCoding, boolean anc) {
        Timing t = get(anc);
        final long bootTimeNanoSeconds = System.nanoTime() - t.bootStartTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        //Use a BigDecimal so we can render in seconds with 3 digits precision, as requested:
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(bootTimeNanoSeconds);
        String safeAppName = (name == null || name.trim().isEmpty()) ? UNSET_VALUE : name;
        String safeAppVersion = (version == null || version.trim().isEmpty()) ? UNSET_VALUE : version;
        final String nativeOrJvm = ImageInfo.inImageRuntimeCode() ? "native" : "on JVM";
        if (UNSET_VALUE.equals(safeAppName) || UNSET_VALUE.equals(safeAppVersion)) {
            logger.infof("Quarkus %s %s started in %ss. %s", quarkusVersion, nativeOrJvm, secondsRepresentation,
                    t.httpServerInfo);
        } else {
            logger.infof("%s %s %s (powered by Quarkus %s) started in %ss. %s", name, version, nativeOrJvm, quarkusVersion,
                    secondsRepresentation, t.httpServerInfo);
        }
        logger.infof("Profile %s activated. %s", profile, liveCoding ? "Live Coding activated." : "");
        logger.infof("Installed features: [%s]", features);
        t.bootStartTime = -1;
    }

    public static void printStopTime(String name, boolean auxiliaryApplication) {
        Timing t = get(auxiliaryApplication);
        final long stopTimeNanoSeconds = System.nanoTime() - t.bootStopTime;
        final Logger logger = Logger.getLogger("io.quarkus");
        final BigDecimal secondsRepresentation = convertToBigDecimalSeconds(stopTimeNanoSeconds);
        logger.infof("%s stopped in %ss",
                (UNSET_VALUE.equals(name) || name == null || name.trim().isEmpty()) ? "Quarkus" : name,
                secondsRepresentation);
        t.bootStopTime = -1;

        if (!auxiliaryApplication) {
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
    }

    public static BigDecimal convertToBigDecimalSeconds(final long timeNanoSeconds) {
        final BigDecimal secondsRepresentation = BigDecimal.valueOf(timeNanoSeconds) // As nanoseconds
                .divide(BigDecimal.valueOf(1_000_000), RoundingMode.HALF_UP) // Convert to milliseconds, discard remaining digits while rounding
                .divide(BigDecimal.valueOf(1_000), 3, RoundingMode.HALF_UP); // Convert to seconds, while preserving 3 digits
        return secondsRepresentation;
    }

}
