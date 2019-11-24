package io.quarkus.runtime.configuration;

import java.util.NoSuchElementException;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.RecomputeFieldValue;

/**
 * Utility methods to log configuration problems.
 */
public final class ConfigDiagnostic {
    private static final Logger log = Logger.getLogger("io.quarkus.config");

    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static volatile boolean error = false;

    private ConfigDiagnostic() {
    }

    public static void invalidValue(String name, IllegalArgumentException ex) {
        final String message = ex.getMessage();
        log.errorf("An invalid value was given for configuration key \"%s\": %s", name,
                message == null ? ex.toString() : message);
        error = true;
    }

    public static void missingValue(String name, NoSuchElementException ex) {
        final String message = ex.getMessage();
        log.errorf("Configuration key \"%s\" is required, but its value is empty/missing: %s", name,
                message == null ? ex.toString() : message);
        error = true;
    }

    public static void duplicate(String name) {
        log.errorf("Configuration key \"%s\" was specified more than once", name);
        error = true;
    }

    public static void deprecated(String name) {
        log.warnf("Configuration key \"%s\" is deprecated", name);
    }

    public static void unknown(String name) {
        log.warnf("Unrecognized configuration key \"%s\" was provided; it will be ignored", name);
    }

    public static void unknown(NameIterator name) {
        unknown(name.getName());
    }

    public static void unknownRunTime(String name) {
        if (ImageInfo.inImageRuntimeCode()) {
            // only warn at run time for native images, otherwise the user will get warned twice for every property
            log.warnf("Unrecognized configuration key \"%s\" was provided; it will be ignored", name);
        }
    }

    public static void unknownRunTime(NameIterator name) {
        unknownRunTime(name.getName());
    }

    /**
     * Determine if a fatal configuration error has occurred.
     *
     * @return {@code true} if a fatal configuration error has occurred
     */
    public static boolean isError() {
        return error;
    }

    /**
     * Reset the config error status (for e.g. testing).
     */
    public static void resetError() {
        error = false;
    }
}
