package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.util.regex.Pattern;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to support regular expressions.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class RegexConverter implements Converter<Pattern> {

    /**
     * Construct a new instance.
     */
    public RegexConverter() {
    }

    public Pattern convert(final String value) {
        return value.isEmpty() ? null : Pattern.compile(value);
    }
}
