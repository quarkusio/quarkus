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
    private static final String PERIOD_OF_TIME = "PT";
    private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");
    private static final Pattern START_WITH_DIGITS = Pattern.compile("^[-+]?\\d+.*");

    public DurationConverter() {
    }

    /**
     * The converter accepts a value which start with a number by implicitly appending `PT` to it.
     * If the value consists only of a number, it implicitly treats the value as seconds.
     * Otherwise, tries to convert the value assuming that it is in the accepted ISO-8601 duration format.
     *
     * @param value duration as String
     * @return {@link Duration}
     */
    @Override
    public Duration convert(String value) {
        return parseDuration(value);
    }

    /**
     * Converts a value which start with a number by implicitly appending `PT` to it.
     * If the value consists only of a number, it implicitly treats the value as seconds.
     * Otherwise, tries to convert the value assuming that it is in the accepted ISO-8601 duration format.
     *
     * @param value duration as String
     * @return {@link Duration}
     */
    public static Duration parseDuration(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (DIGITS.asPredicate().test(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        }

        try {
            if (START_WITH_DIGITS.asPredicate().test(value)) {
                return Duration.parse(PERIOD_OF_TIME + value);
            }

            return Duration.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
