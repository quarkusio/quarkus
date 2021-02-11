package io.quarkus.scheduler.runtime.util;

import java.time.Duration;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.scheduler.Scheduled;

/**
 * Utilities class for scheduler extensions.
 *
 */
public class SchedulerUtils {

    private static final String DELAYED = "delayed";
    private static final String EVERY = "every";

    private SchedulerUtils() {

    }

    /**
     * Parse the `@Scheduled(delayed = "")` field into milliseconds.
     *
     * @param scheduled annotation
     * @return returns the duration in milliseconds.
     */
    public static long parseDelayedAsMillis(Scheduled scheduled) {
        return parseDurationAsMillis(scheduled, scheduled.delayed(), DELAYED);
    }

    /**
     * Parse the `@Scheduled(every = "")` field into milliseconds.
     *
     * @param scheduled annotation
     * @return returns the duration in milliseconds.
     */
    public static long parseEveryAsMillis(Scheduled scheduled) {
        return parseDurationAsMillis(scheduled, scheduled.every(), EVERY);
    }

    /**
     * Looks up the property value by checking whether the value is a configuration key and resolves it if so.
     *
     * @param propertyValue property value to look up.
     * @return the resolved property value.
     */
    public static String lookUpPropertyValue(String propertyValue) {
        String value = propertyValue.trim();
        if (!value.isEmpty() && isConfigValue(value)) {
            value = ConfigProviderResolver.instance().getConfig().getValue(getConfigProperty(value), String.class);
        }

        return value;
    }

    public static boolean isConfigValue(String val) {
        val = val.trim();
        return val.startsWith("{") && val.endsWith("}");
    }

    private static String getConfigProperty(String val) {
        return val.substring(1, val.length() - 1);
    }

    private static long parseDurationAsMillis(Scheduled scheduled, String value, String memberName) {
        return Math.abs(parseDuration(scheduled, value, memberName).toMillis());
    }

    private static Duration parseDuration(Scheduled scheduled, String value, String memberName) {
        value = lookUpPropertyValue(value);
        if (Character.isDigit(value.charAt(0))) {
            value = "PT" + value;
        }

        try {
            return Duration.parse(value);
        } catch (Exception e) {
            // This could only happen for config-based expressions
            throw new IllegalStateException("Invalid " + memberName + "() expression on: " + scheduled, e);
        }
    }
}
