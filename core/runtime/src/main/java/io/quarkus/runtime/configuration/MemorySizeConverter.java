package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to support data sizes.
 *
 * @deprecated Use {@link MemorySize#of(String)} directly; this converter is retained only for
 *             backward compatibility with code that references it explicitly.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
@Deprecated(forRemoval = true)
public class MemorySizeConverter implements Converter<MemorySize>, Serializable {
    private static final long serialVersionUID = -1988485929047973068L;

    /**
     * {@inheritDoc}
     */
    @Override
    public MemorySize convert(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        return MemorySize.of(value);
    }
}
