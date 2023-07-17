package io.quarkus.resteasy.common.runtime.config;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.resteasy.spi.config.SizeUnit;
import org.jboss.resteasy.spi.config.Threshold;

/**
 * A converter for a {@link Threshold} interface.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class ThresholdConverter implements Converter<Threshold>, Serializable {

    public static final Threshold NONE = Threshold.of(-1L, SizeUnit.BYTE);
    public static final Threshold DEFAULT = Threshold.of(512L, SizeUnit.KILOBYTE);
    private static final Pattern PATTERN = Pattern.compile("(?<size>-?(?!0)\\d+)\\s*(?<unit>(?:ZB|EB|TB|PB|GB|MB|KB|B)\\b)?");

    public ThresholdConverter() {
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
    public Threshold convert(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }

        // The value should be something like 1 MB or 1MB
        final Matcher matcher = PATTERN.matcher(value.toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            return DEFAULT;
        }
        final String stringSize = matcher.group("size");
        final String stringUnit = matcher.group("unit");
        final long size;
        if (stringSize == null || stringSize.isBlank()) {
            return DEFAULT;
        } else {
            size = Long.parseLong(stringSize);
        }
        if (size < 0L) {
            return NONE;
        }
        SizeUnit unit = null;
        for (SizeUnit u : SizeUnit.values()) {
            if (u.abbreviation().equals(stringUnit)) {
                unit = u;
                break;
            }
        }
        return Threshold.of(size, unit == null ? SizeUnit.BYTE : unit);
    }
}
