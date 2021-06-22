package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
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
        final String loggedMessage = message != null ? message
                : String.format("An invalid value was given for configuration key \"%s\"", name);
        errorsMessages.add(loggedMessage);
    }

    public static void missingValue(String name, NoSuchElementException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = message != null ? message
                : String.format("Configuration key \"%s\" is required, but its value is empty/missing", name);
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
        log.warnf(
                "Unrecognized configuration key \"%s\" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo",
                name);
    }

    public static void unknown(NameIterator name) {
        unknown(name.getName());
    }

    /**
     * Report any unused properties.
     *
     * The list of unused properties may contain false positives. This is caused when an environment variable is set up
     * and we cannot determine correctly if it was used or not.
     *
     * Environment variables required conversion to regular property names so a Map can be properly populated when
     * iterating {@link Config#getPropertyNames()}. Because an Environment variable name may match multiple property
     * names, we try a best effort to report unknowns by matching used properties in their Environment variable name
     * format.
     *
     * @param properties the list of possible unused properties
     */
    public static void unknownProperties(List<String> properties) {
        List<String> usedProperties = new ArrayList<>();
        for (String property : ConfigProvider.getConfig().getPropertyNames()) {
            if (properties.contains(property)) {
                continue;
            }

            usedProperties.add(replaceNonAlphanumericByUnderscores(property));
        }
        usedProperties.removeAll(properties);

        for (String property : properties) {
            boolean found = false;
            for (String usedProperty : usedProperties) {
                if (usedProperty.equalsIgnoreCase(replaceNonAlphanumericByUnderscores(property))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                unknown(property);
            }
        }
    }

    public static void unknownRunTime(String name) {
        if (ImageInfo.inImageRuntimeCode()) {
            // only warn at run time for native images, otherwise the user will get warned twice for every property
            unknown(name);
        }
    }

    public static void unknownRunTime(NameIterator name) {
        unknownRunTime(name.getName());
    }

    public static void unknownPropertiesRuntime(List<String> properties) {
        if (ImageInfo.inImageRuntimeCode()) {
            unknownProperties(properties);
        }
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
        StringBuilder b = new StringBuilder();
        for (String errorsMessage : errorsMessages) {
            b.append("  - ");
            b.append(errorsMessage);
            b.append(System.lineSeparator());
        }
        return b.toString();
    }

    private static String replaceNonAlphanumericByUnderscores(final String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if ('a' <= c && c <= 'z' ||
                    'A' <= c && c <= 'Z' ||
                    '0' <= c && c <= '9') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
