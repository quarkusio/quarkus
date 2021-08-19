package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to normalize paths that are considered root of something.
 * <p>
 * Any path coming out of this converter will have a leading and ending '/'.
 * <p>
 * Do NOT use this converter for paths that could be relative.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class NormalizeRootHttpPathConverter implements Converter<String> {

    private static final String SLASH = "/";

    @Override
    public String convert(String value) throws IllegalArgumentException, NullPointerException {
        if (value == null) {
            return SLASH;
        }

        value = value.trim();
        if (SLASH.equals(value)) {
            return value;
        }
        if (!value.startsWith(SLASH)) {
            value = SLASH + value;
        }
        if (!value.endsWith(SLASH)) {
            value = value + SLASH;
        }

        return value;
    }
}
