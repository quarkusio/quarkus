package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.time.Duration;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter for a {@link Duration} interface.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class DurationConverter implements Converter<Duration>, Serializable {
    private static final long serialVersionUID = 7499347081928776532L;

    private static final String PERIOD = "P";
    private static final String PERIOD_OF_TIME = "PT";

    @Override
    public Duration convert(String value) {
        return parseDuration(value);
    }

    public static Duration parseDuration(String value) {
        value = value.trim();
        int len = value.length();
        if (len == 0)
            return null;

        // only numbers, these are seconds
        if (isNumeric(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        }

        char last = value.charAt(len - 1);
        char lastLower = Character.toLowerCase(last);

        // single letter units, we only handle integers, decimal numbers will be handled by Duration#parse()
        String numericPart = value.substring(0, len - 1);
        if (isNumeric(numericPart)) {
            long val = Long.parseLong(numericPart);
            switch (lastLower) {
                case 's':
                    return Duration.ofSeconds(val);
                case 'm':
                    return Duration.ofMinutes(val);
                case 'h':
                    return Duration.ofHours(val);
                case 'd':
                    return Duration.ofDays(val);
            }
        }

        // specific case of milliseconds
        if (len > 2 && lastLower == 's' && Character.toLowerCase(value.charAt(len - 2)) == 'm') {
            String num = value.substring(0, len - 2);
            if (isNumeric(num))
                return Duration.ofMillis(Long.parseLong(num));
        }

        // cases handled by Duration.parse(), we add the P/PT prefix if needed
        try {
            if (isDecimal(numericPart)) {
                if (lastLower == 'h' || lastLower == 'm' || lastLower == 's') {
                    return Duration.parse(PERIOD_OF_TIME + value);
                } else if (lastLower == 'd') {
                    return Duration.parse(PERIOD + value);
                }
            }
            // Handle "1h30m" style (DIGITS_AND_UNIT)
            if (startsLikeDigits(value)) {
                return Duration.parse(PERIOD_OF_TIME + value);
            }
            return Duration.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid duration: " + value, e);
        }
    }

    private static boolean isNumeric(String s) {
        int len = s.length();
        if (len == 0) {
            return false;
        }
        int i = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;
        if (i == len) {
            return false;
        }
        for (; i < len; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isDecimal(String s) {
        int len = s.length();
        if (len == 0) {
            return false;
        }
        boolean hasDot = false;
        int i = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;
        for (; i < len; i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (hasDot) {
                    return false;
                }
                hasDot = true;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return hasDot;
    }

    private static boolean startsLikeDigits(String s) {
        char c = s.charAt(0);
        if (c >= '0' && c <= '9') {
            return true;
        }
        if (c == '-' || c == '+') {
            // Check if it's already an ISO-8601 duration (has P after optional sign)
            int signOffset = 1;
            return signOffset >= s.length() || s.charAt(signOffset) != 'P';
        }
        return false;
    }
}
