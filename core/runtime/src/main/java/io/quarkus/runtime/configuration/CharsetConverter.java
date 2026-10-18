package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.nio.charset.Charset;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter which converts a Charset string into an instance of {@link java.nio.charset.Charset}.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class CharsetConverter implements Converter<Charset>, Serializable {

    private static final long serialVersionUID = 2320905063828247874L;

    @Override
    public Charset convert(String value) {
        if (value == null) {
            return null;
        }

        String trimmedCharset = value.trim();

        if (trimmedCharset.isEmpty()) {
            return null;
        }

        try {
            return Charset.forName(trimmedCharset);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create Charset from: '" + trimmedCharset + "'", e);
        }
    }
}
