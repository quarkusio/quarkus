package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to support regular expressions.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class RegexConverter implements Converter<Pattern>, Serializable {

    private static final long serialVersionUID = -2627801624423530576L;

    /**
     * Construct a new instance.
     */
    public RegexConverter() {
    }

    public Pattern convert(final String value) {
        return value.isEmpty() ? null : Pattern.compile(value);
    }
}
