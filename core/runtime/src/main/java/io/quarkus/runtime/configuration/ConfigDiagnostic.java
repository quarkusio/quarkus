package io.quarkus.runtime.configuration;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.RecomputeFieldValue;

/**
 * Utility methods to log configuration problems.
 */
public final class ConfigDiagnostic {
    private static final Logger log = Logger.getLogger("io.quarkus.config");

    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static final List<String> errorsMessages = new CopyOnWriteArrayList<>();

    private ConfigDiagnostic() {
    }

    public static void invalidValue(String name, IllegalArgumentException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = String.format("An invalid value was given for configuration key \"%s\": %s", name,
                message == null ? ex.toString() : message);
        log.error(loggedMessage);
        errorsMessages.add(loggedMessage);
    }

    public static void missingValue(String name, NoSuchElementException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = String.format("Configuration key \"%s\" is required, but its value is empty/missing: %s",
                name,
                message == null ? ex.toString() : message);
        log.error(loggedMessage);
        errorsMessages.add(loggedMessage);
    }

    public static void duplicate(String name) {
        final String loggedMessage = String.format("Configuration key \"%s\" was specified more than once", name);
        errorsMessages.add(loggedMessage);
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
        return !errorsMessages.isEmpty();
    }

    /**
     * Reset the config error status (for e.g. testing).
     */
    public static void resetError() {
        errorsMessages.clear();
    }

    public static String getNiceErrorMessage() {
        return String.join("\n", errorsMessages);
    }
}
