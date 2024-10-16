package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

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
    public static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");
    private static final Pattern DIGITS_AND_UNIT = Pattern.compile("^(?:[-+]?\\d+(?:\\.\\d+)?(?i)[hms])+$");
    private static final Pattern DAYS = Pattern.compile("^[-+]?\\d+(?i)d$");
    private static final Pattern MILLIS = Pattern.compile("^[-+]?\\d+(?i)ms$");

    public DurationConverter() {
    }

    /**
     * If the {@code value} starts with a number, then:
     * <ul>
     * <li>If the value is only a number, it is treated as a number of seconds.</li>
     * <li>If the value is a number followed by {@code ms}, it is treated as a number of milliseconds.</li>
     * <li>If the value is a number followed by {@code h}, {@code m}, or {@code s}, it is prefixed with {@code PT}
     * and {@link Duration#parse(CharSequence)} is called.</li>
     * <li>If the value is a number followed by {@code d}, it is prefixed with {@code P}
     * and {@link Duration#parse(CharSequence)} is called.</li>
     * </ul>
     *
     * Otherwise, {@link Duration#parse(CharSequence)} is called.
     *
     * @param value a string duration
     * @return the parsed {@link Duration}
     * @throws IllegalArgumentException in case of parse failure
     */
    @Override
    public Duration convert(String value) {
        return parseDuration(value);
    }

    /**
     * If the {@code value} starts with a number, then:
     * <ul>
     * <li>If the value is only a number, it is treated as a number of seconds.</li>
     * <li>If the value is a number followed by {@code ms}, it is treated as a number of milliseconds.</li>
     * <li>If the value is a number followed by {@code h}, {@code m}, or {@code s}, it is prefixed with {@code PT}
     * and {@link Duration#parse(CharSequence)} is called.</li>
     * <li>If the value is a number followed by {@code d}, it is prefixed with {@code P}
     * and {@link Duration#parse(CharSequence)} is called.</li>
     * </ul>
     *
     * Otherwise, {@link Duration#parse(CharSequence)} is called.
     *
     * @param value a string duration
     * @return the parsed {@link Duration}
     * @throws IllegalArgumentException in case of parse failure
     */
    public static Duration parseDuration(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (DIGITS.asPredicate().test(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        } else if (MILLIS.asPredicate().test(value)) {
            return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
        }

        try {
            if (DIGITS_AND_UNIT.asPredicate().test(value)) {
                return Duration.parse(PERIOD_OF_TIME + value);
            } else if (DAYS.asPredicate().test(value)) {
                return Duration.parse(PERIOD + value);
            }

            return Duration.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
