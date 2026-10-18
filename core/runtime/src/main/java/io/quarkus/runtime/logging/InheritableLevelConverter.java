package io.quarkus.runtime.logging;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A simple converter for inheritable logging levels.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public final class InheritableLevelConverter implements Converter<InheritableLevel>, Serializable {

    private static final long serialVersionUID = 704275577610445233L;

    public InheritableLevel convert(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return InheritableLevel.of(value);
    }
}
