package io.quarkus.runtime.configuration;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ImageMode;
import io.smallrye.config.common.utils.StringUtil;

/**
 * Utility methods to log configuration problems.
 */
public final class ConfigDiagnostic {
    private static final Logger log = Logger.getLogger("io.quarkus.config");

    private static final List<String> errorsMessages = new CopyOnWriteArrayList<>();
    private static final Set<String> errorKeys = new CopyOnWriteArraySet<>();

    private ConfigDiagnostic() {
    }

    public static void invalidValue(String name, IllegalArgumentException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = message != null ? message
                : String.format("An invalid value was given for configuration key \"%s\"", name);
        errorsMessages.add(loggedMessage);
        errorKeys.add(name);
    }

    public static void missingValue(String name, NoSuchElementException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = message != null ? message
                : String.format("Configuration key \"%s\" is required, but its value is empty/missing", name);
        errorsMessages.add(loggedMessage);
        errorKeys.add(name);
    }

    public static void duplicate(String name) {
        final String loggedMessage = String.format("Configuration key \"%s\" was specified more than once", name);
        errorsMessages.add(loggedMessage);
        errorKeys.add(name);
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
     * <br>
     * The list of unused properties may contain false positives. This is caused when an environment variable is set up,
     * and we cannot determine correctly if it was used or not.
     * <br>
     * Environment variables require a conversion to regular property names so a Map can be properly populated when
     * iterating {@link Config#getPropertyNames()}. Because an Environment variable name may match multiple property
     * names, we try the best effort to report unknowns by matching used properties in their Environment variable name
     * format.
     *
     * @param properties the set of possible unused properties
     */
    public static void unknownProperties(Set<String> properties) {
        Set<String> usedProperties = new HashSet<>();
        for (String property : ConfigProvider.getConfig().getPropertyNames()) {
            if (properties.contains(property)) {
                continue;
            }

            usedProperties.add(StringUtil.replaceNonAlphanumericByUnderscores(property));
        }
        usedProperties.removeAll(properties);

        for (String property : properties) {
            // Indexed properties not supported by @ConfigRoot, but they can show up due to the YAML source. Just ignore them.
            if (property.contains("[") && property.contains("]")) {
                continue;
            }

            boolean found = false;
            for (String usedProperty : usedProperties) {
                if (usedProperty.equalsIgnoreCase(StringUtil.replaceNonAlphanumericByUnderscores(property))) {
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
        if (ImageMode.current() == ImageMode.NATIVE_RUN) {
            // only warn at run time for native images, otherwise the user will get warned twice for every property
            unknown(name);
        }
    }

    public static void unknownRunTime(NameIterator name) {
        unknownRunTime(name.getName());
    }

    public static void unknownPropertiesRuntime(Set<String> properties) {
        if (ImageMode.current() == ImageMode.NATIVE_RUN) {
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
        errorKeys.clear();
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

    public static Set<String> getErrorKeys() {
        return new HashSet<>(errorKeys);
    }
}
