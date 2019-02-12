package org.jboss.shamrock.runtime.configuration;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to support regular expressions.
 */
public class RegexConverter implements Converter<Pattern> {
    /**
     * Construct a new instance.
     */
    public RegexConverter() {}

    public Pattern convert(final String value) {
        return Pattern.compile(value);
    }
}
